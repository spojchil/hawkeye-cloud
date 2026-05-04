package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量上下文 — 分层变量解析。
 * <p>
 * 优先级：Extractor 提取变量 > 模板级 variables > Asset 占位符 > 随机函数
 * <p>
 * 支持的变量格式：
 * - {{变量名}} - 标准变量
 * - {{函数名(参数)}} - 函数调用（如 rand_int, rand_base, randstr）
 * - ${{变量名}} - Nuclei 兼容格式（简化处理，只解析内部的变量）
 */
public class VariableContext {

    private static final Pattern VAR = Pattern.compile("\\{\\{(.+?)}}");
    // 匹配 ${{变量名}} 格式
    private static final Pattern NUCLEI_VAR = Pattern.compile("\\$\\{\\{(.+?)}}");
    // 匹配 ${:-变量名} 格式（Nuclei 默认值语法）
    private static final Pattern NUCLEI_DEFAULT = Pattern.compile("\\$\\{:-([^}]+)}");

    private final Map<String, Object> extractorVars = new HashMap<>();
    private final Map<String, Object> templateVars;
    private final String baseURL;
    private final String hostname;
    private final String rootURL;

    public VariableContext(TaskItemMessage msg) {
        this.templateVars = msg.getVariables() != null ? new HashMap<>(msg.getVariables()) : Map.of();
        String protocol = msg.getAssetProtocol() != null ? msg.getAssetProtocol() : "https";
        String host = msg.getAssetHost() != null ? msg.getAssetHost() : "";
        int port = msg.getAssetPort() != null ? msg.getAssetPort() : ("https".equals(protocol) ? 443 : 80);
        String path = msg.getAssetPath() != null ? msg.getAssetPath() : "/";

        boolean isDefaultPort = ("http".equals(protocol) && port == 80) ||
                               ("https".equals(protocol) && port == 443);
        String portStr = isDefaultPort ? "" : ":" + port;

        this.baseURL = protocol + "://" + host + portStr;
        this.hostname = host;
        this.rootURL = protocol + "://" + host + portStr + "/";
    }

    public void set(String key, Object value) {
        extractorVars.put(key, value);
    }

    public Object get(String key) {
        Object v = extractorVars.get(key);
        if (v != null) return v;
        v = templateVars.get(key);
        if (v != null) return v;
        return null;
    }

    /**
     * 解析模板中的变量占位符。
     * <p>
     * 支持格式：
     * - {{变量名}}
     * - ${{变量名}}（Nuclei 兼容）
     * - ${:-变量名}（Nuclei 默认值语法，简化处理为变量名）
     */
    public String resolve(String template) {
        if (template == null) return null;
        
        String result = template;
        
        // 1. 解析 ${{变量名}} 格式（Nuclei 兼容）
        Matcher nucleiMatcher = NUCLEI_VAR.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (nucleiMatcher.find()) {
            String varName = nucleiMatcher.group(1).trim();
            nucleiMatcher.appendReplacement(sb, Matcher.quoteReplacement(evalBuiltin(varName)));
        }
        nucleiMatcher.appendTail(sb);
        result = sb.toString();
        
        // 2. 解析 ${:-变量名} 格式（Nuclei 默认值语法）
        Matcher defaultMatcher = NUCLEI_DEFAULT.matcher(result);
        sb = new StringBuilder();
        while (defaultMatcher.find()) {
            String varName = defaultMatcher.group(1).trim();
            // 尝试解析变量，如果不存在则保留原样
            Object value = get(varName);
            String replacement = value != null ? value.toString() : defaultMatcher.group(0);
            defaultMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        defaultMatcher.appendTail(sb);
        result = sb.toString();
        
        // 3. 解析标准 {{变量名}} 格式
        Matcher m = VAR.matcher(result);
        sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1).trim();
            m.appendReplacement(sb, Matcher.quoteReplacement(evalBuiltin(varName)));
        }
        m.appendTail(sb);
        result = sb.toString();
        
        return result;
    }

    private String evalBuiltin(String name) {
        // Asset 占位符
        return switch (name) {
            case "BaseURL" -> baseURL;
            case "Hostname" -> hostname;
            case "RootURL" -> rootURL;
            default -> {
                // rand_int / rand_base / randstr
                if (name.startsWith("rand_int(")) {
                    yield String.valueOf(evalRandInt(name));
                } else if (name.startsWith("rand_base(")) {
                    yield evalRandBase(name);
                } else if ("randstr".equals(name)) {
                    yield Long.toHexString(ThreadLocalRandom.current().nextLong());
                }
                // interactsh-url 是 OAST 机制，生成一个随机域名
                if ("interactsh-url".equals(name)) {
                    yield generateInteractshUrl();
                }
                // 变量查找
                Object v = get(name.trim());
                yield v != null ? v.toString() : "{{" + name + "}}";
            }
        };
    }

    // TODO 你这里很奇怪，本来就不实现，而且我发现上次的竟然是检测结果竟然是，未命中，这就给客户一个错觉就是没问题，但是实际上应该返回的是不支持
    /**
     * 生成 interactsh-url（简化实现）。
     * <p>
     * 原始实现需要与 interact.sh 服务器交互，这里简化为生成随机域名。
     * 注意：这只是为了模板能正常执行，OAST 检测功能不会生效。
     */
    private String generateInteractshUrl() {
        String random = Long.toHexString(ThreadLocalRandom.current().nextLong());
        return random + ".interact.sh";
    }

    private long evalRandInt(String expr) {
        try {
            String inner = expr.substring(expr.indexOf('(') + 1, expr.indexOf(')'));
            if (inner.contains(",")) {
                String[] parts = inner.split(",");
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return ThreadLocalRandom.current().nextLong(min, max + 1);
            }
            return ThreadLocalRandom.current().nextLong(Integer.parseInt(inner.trim()));
        } catch (Exception e) {
            return 0;
        }
    }

    private String evalRandBase(String expr) {
        try {
            String inner = expr.substring(expr.indexOf('(') + 1, expr.indexOf(')'));
            int len = Integer.parseInt(inner.trim());
            StringBuilder sb = new StringBuilder();
            String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            for (int i = 0; i < len; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 当前步响应后同步运行时变量。
     */
    public void updateFrom(HttpResponseContext ctx) {
        set("body", ctx.getBody() != null ? ctx.getBody() : "");
        set("status_code", ctx.getStatusCode());
        set("content_type", ctx.getContentType() != null ? ctx.getContentType() : "");
        set("content_length", ctx.getContentLength());
        set("duration", ctx.getDurationMs());
    }
}

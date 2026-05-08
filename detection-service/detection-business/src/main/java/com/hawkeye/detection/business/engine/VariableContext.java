package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量上下文——占位符解析 + 内置函数 + 跨步骤变量回写
 *
 * <p>变量优先级：提取器变量 > 模板变量 > 消息内置变量（BaseURL/Hostname/RootURL）。
 * 支持 Nuclei 风格的 ${...} 和 {{...}} 两种占位符，嵌套解析最多 2 次。</p>
 */
@Slf4j
public class VariableContext {

    private static final Pattern VAR = Pattern.compile("\\{\\{(.+?)}}");
    private static final Pattern NUCLEI_VAR = Pattern.compile("\\$\\{\\{(.+?)}}");
    private static final Pattern NUCLEI_DEFAULT = Pattern.compile("\\$\\{:-([^}]+)}");

    /* 提取器回写的变量（优先级最高） */
    private final Map<String, Object> extractorVars = new HashMap<>();
    /* 模板定义的变量 */
    private final Map<String, Object> templateVars;
    /* 消息内置变量 */
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
     * 解析两次以支持嵌套引用（如 {{num1}} → {{rand_int(800000, 999999)}}）。
     */
    public String resolve(String template) {
        if (template == null) return null;
        String result = template;
        result = resolveNucleiVar(result);
        result = resolveNucleiDefault(result);
        result = resolveStandardVar(result);
        result = resolveStandardVar(result);  /* 二次解析，处理嵌套 */
        return result;
    }

    private String resolveNucleiVar(String template) {
        Matcher m = NUCLEI_VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1).trim();
            m.appendReplacement(sb, Matcher.quoteReplacement(evalBuiltin(varName)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String resolveNucleiDefault(String template) {
        Matcher m = NUCLEI_DEFAULT.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1).trim();
            Object value = get(varName);
            String replacement = value != null ? value.toString() : m.group(0);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String resolveStandardVar(String template) {
        Matcher m = VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1).trim();
            m.appendReplacement(sb, Matcher.quoteReplacement(evalBuiltin(varName)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /* 解析内置变量和自定义变量 */
    private String evalBuiltin(String name) {
        return switch (name) {
            case "BaseURL" -> baseURL;
            case "Hostname" -> hostname;
            case "RootURL" -> rootURL;
            default -> {
                if (name.startsWith("rand_int(")) {
                    yield String.valueOf(evalRandInt(name));
                } else if (name.startsWith("rand_base(")) {
                    yield evalRandBase(name);
                } else if ("randstr".equals(name)) {
                    yield Long.toHexString(ThreadLocalRandom.current().nextLong());
                }
                Object v = get(name.trim());
                yield v != null ? v.toString() : "{{" + name + "}}";
            }
        };
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
            log.warn("解析 rand_int 失败: {}", expr, e);
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
            log.warn("解析 rand_base 失败: {}", expr, e);
            return "";
        }
    }

    /* 将 HTTP 响应信息写入变量上下文 */
    public void updateFrom(HttpResponseContext ctx) {
        set("body", ctx.getBody() != null ? ctx.getBody() : "");
        set("status_code", ctx.getStatusCode());
        set("content_type", ctx.getContentType() != null ? ctx.getContentType() : "");
        set("content_length", ctx.getContentLength());
        set("duration", ctx.getDurationMs());
    }
}

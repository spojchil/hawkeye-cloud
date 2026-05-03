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
 * 优先级：Extractor 提取变量 > 模板级 variables > Asset 占位符 > 随机函数
 */
public class VariableContext {

    private static final Pattern VAR = Pattern.compile("\\{\\{(.+?)}}");

    private final Map<String, Object> extractorVars = new HashMap<>();
    private final Map<String, Object> templateVars;
    private final String baseURL;
    private final String hostname;
    private final String rootURL;

    public VariableContext(TaskItemMessage msg) {
        this.templateVars = msg.getVariables() != null ? new HashMap<>(msg.getVariables()) : Map.of();
        String protocol = msg.getAssetProtocol() != null ? msg.getAssetProtocol() : "https";
        String host = msg.getAssetHost() != null ? msg.getAssetHost() : "";
        int port = msg.getAssetPort() != null ? msg.getAssetPort() : 443;
        String path = msg.getAssetPath() != null ? msg.getAssetPath() : "/";
        this.baseURL = protocol + "://" + host + (port == 443 || port == 80 ? "" : ":" + port);
        this.hostname = host;
        this.rootURL = protocol + "://" + host + (port == 443 || port == 80 ? "" : ":" + port) + "/";
    }

    public void set(String key, Object value) { extractorVars.put(key, value); }

    public Object get(String key) {
        Object v = extractorVars.get(key);
        if (v != null) return v;
        v = templateVars.get(key);
        if (v != null) return v;
        return null;
    }

    /** 解析模板中的 {{...}} 占位符 */
    public String resolve(String template) {
        if (template == null) return null;
        Matcher m = VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1).trim();
            m.appendReplacement(sb, Matcher.quoteReplacement(evalBuiltin(varName)));
        }
        m.appendTail(sb);
        return sb.toString();
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
                // 变量查找
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

    /** 当前步响应后同步运行时变量 */
    public void updateFrom(HttpResponseContext ctx) {
        set("body", ctx.getBody() != null ? ctx.getBody() : "");
        set("status_code", ctx.getStatusCode());
        set("content_type", ctx.getContentType() != null ? ctx.getContentType() : "");
        set("content_length", ctx.getContentLength());
        set("duration", ctx.getDurationMs());
    }
}

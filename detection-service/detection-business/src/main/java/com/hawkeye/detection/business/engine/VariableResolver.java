package com.hawkeye.detection.business.engine;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量解析器 — 将模板占位符替换为实际值。
 * <p>
 * 优先级：Extractor 提取变量 > 模板 variables > Asset 内置占位符 > 随机生成函数
 */
public class VariableResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+(?:\\([^)]*\\))?)\\}\\}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, Object> context = new HashMap<>();

    private final String protocol;
    private final String host;
    private final int port;
    private final String path;

    public VariableResolver(String protocol, String host, Integer port, String path,
                            Map<String, Object> templateVars) {
        this.protocol = protocol != null ? protocol : "https";
        this.host = host != null ? host : "";
        this.port = port != null ? port : 443;
        this.path = path != null ? path : "/";
        if (templateVars != null) {
            context.putAll(templateVars);
        }
    }

    public void put(String key, Object value) {
        context.put(key, value);
    }

    public Object get(String key) {
        return context.get(key);
    }

    /** 替换字符串中所有 {{...}} 占位符 */
    public String resolve(String input) {
        if (input == null) return null;
        Matcher m = PLACEHOLDER.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String expr = m.group(1);
            String replacement = resolveExpr(expr);
            m.appendReplacement(sb, replacement != null ? Matcher.quoteReplacement(replacement) : "");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String resolveExpr(String expr) {
        if ("BaseURL".equalsIgnoreCase(expr)) return buildBaseUrl();
        if ("Hostname".equalsIgnoreCase(expr)) return host;
        if ("RootURL".equalsIgnoreCase(expr)) return buildRootUrl();
        if ("Port".equalsIgnoreCase(expr)) return String.valueOf(port);
        if ("Path".equalsIgnoreCase(expr)) return path;

        if (expr.startsWith("rand_base(")) return randomAlphanumeric(extractIntArg(expr));
        if ("randstr".equals(expr)) return randomLowercase(8);
        if (expr.startsWith("rand_int(")) return String.valueOf(ThreadLocalRandom.current().nextInt(extractIntArg(expr)));

        Object val = context.get(expr);
        return val != null ? val.toString() : "";
    }

    private String buildBaseUrl() {
        StringBuilder sb = new StringBuilder(buildRootUrl());
        sb.append(path.startsWith("/") ? path : "/" + path);
        return sb.toString();
    }

    private String buildRootUrl() {
        StringBuilder sb = new StringBuilder(protocol).append("://").append(host);
        if (!((protocol.equals("https") && port == 443) || (protocol.equals("http") && port == 80))) {
            sb.append(":").append(port);
        }
        return sb.toString();
    }

    private int extractIntArg(String expr) {
        Matcher m = Pattern.compile("\\((\\d+)\\)").matcher(expr);
        return m.find() ? Integer.parseInt(m.group(1)) : 8;
    }

    private String randomAlphanumeric(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        return sb.toString();
    }

    private String randomLowercase(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append((char) ('a' + SECURE_RANDOM.nextInt(26)));
        return sb.toString();
    }
}

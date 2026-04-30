package com.hawkeye.detection.business.engine;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.hawkeye.detection.common.pojo.dto.AssetDTO;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量解析器——将模板中的占位符替换为实际值。
 * <p>
 * 优先级：
 * 1. 上一步 Extractor 提取的变量（最高）
 * 2. 模板 variables 字段定义的变量
 * 3. Asset 内置占位符（BaseURL / Hostname / RootURL / Port / Path）
 * 4. 随机生成函数（rand_base / randstr / rand_int）
 */
public class VariableResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+(?:\\([^)]*\\))?)\\}\\}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AssetDTO asset;
    private final VulTemplateDTO template;
    private final Map<String, Object> context;

    public VariableResolver(AssetDTO asset, VulTemplateDTO template) {
        this.asset = asset;
        this.template = template;
        this.context = new HashMap<>();
        initTemplateVars();
    }

    /** 后续步骤写入的变量（从 Extractor 提取） */
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
            m.appendReplacement(sb, replacement != null ? Matcher.quoteReplacement(replacement) : m.group(0));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 解析 {{expression}} 中的表达式 */
    private String resolveExpr(String expr) {
        // 内置占位符
        if ("BaseURL".equals(expr) || "baseurl".equalsIgnoreCase(expr)) {
            return buildBaseUrl();
        }
        if ("Hostname".equals(expr) || "hostname".equalsIgnoreCase(expr)) {
            return asset.getRequestHost();
        }
        if ("RootURL".equals(expr) || "rooturl".equalsIgnoreCase(expr)) {
            return buildRootUrl();
        }
        if ("Port".equals(expr) || "port".equalsIgnoreCase(expr)) {
            return String.valueOf(asset.getRequestPort() != null ? asset.getRequestPort() : 443);
        }
        if ("Path".equals(expr) || "path".equalsIgnoreCase(expr)) {
            return asset.getRequestPath() != null ? asset.getRequestPath() : "/";
        }

        // 随机生成函数: rand_base(N)
        if (expr.startsWith("rand_base(")) {
            int len = extractIntArg(expr);
            return randomAlphanumeric(len);
        }
        if ("randstr".equals(expr)) {
            return randomLowercase(8);
        }
        if (expr.startsWith("rand_int(")) {
            int max = extractIntArg(expr);
            return String.valueOf(ThreadLocalRandom.current().nextInt(max));
        }

        // 上下文变量
        Object val = context.get(expr);
        return val != null ? val.toString() : "";
    }

    private String buildBaseUrl() {
        return buildRootUrl() + (StrUtil.isNotBlank(asset.getRequestPath()) ? asset.getRequestPath() : "/");
    }

    private String buildRootUrl() {
        String protocol = StrUtil.isNotBlank(asset.getRequestProtocol())
                ? asset.getRequestProtocol() : "https";
        int port = asset.getRequestPort() != null ? asset.getRequestPort() : 443;
        if (port == 443 || port == 80) {
            return protocol + "://" + asset.getRequestHost();
        }
        return protocol + "://" + asset.getRequestHost() + ":" + port;
    }

    private void initTemplateVars() {
        Map<String, Object> vars = template.getVariables();
        if (vars != null && !vars.isEmpty()) {
            context.putAll(vars);
        }
    }

    private int extractIntArg(String expr) {
        Matcher m = Pattern.compile("\\((\\d+)\\)").matcher(expr);
        return m.find() ? Integer.parseInt(m.group(1)) : 8;
    }

    private String randomAlphanumeric(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String randomLowercase(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + SECURE_RANDOM.nextInt(26)));
        }
        return sb.toString();
    }

}

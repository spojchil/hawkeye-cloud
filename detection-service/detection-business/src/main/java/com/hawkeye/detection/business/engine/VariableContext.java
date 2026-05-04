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
 * 变量上下文 — 分层变量解析。
 * <p>
 * 负责管理和解析检测过程中的所有变量，包括：
 * - 资产信息（BaseURL、Hostname、Port 等）
 * - 模板变量（用户在模板中定义的变量）
 * - 提取器变量（从 HTTP 响应中提取的变量）
 * - 内置函数（rand_int、rand_base、randstr 等）
 * <p>
 * 变量优先级（从高到低）：
 * 1. Extractor 提取变量（运行时动态提取）
 * 2. 模板级 variables（模板中定义的静态变量）
 * 3. Asset 占位符（BaseURL、Hostname、RootURL）
 * 4. 随机函数（rand_int、rand_base、randstr）
 * <p>
 * 支持的变量格式：
 * <ul>
 *   <li>{@code {{变量名}}} - 标准变量格式</li>
 *   <li>{@code {{函数名(参数)}}} - 函数调用（如 rand_int(1,100)）</li>
 *   <li>{@code ${{变量名}}} - Nuclei 兼容格式</li>
 *   <li>{@code ${:-变量名}} - Nuclei 默认值语法</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *   VariableContext vars = new VariableContext(msg);
 *   String url = vars.resolve("{{BaseURL}}/api/{{rand_int(1,100)}}");
 *   // 结果：http://example.com:8080/api/42
 * </pre>
 */
@Slf4j
public class VariableContext {

    // ── 正则表达式 ──────────────────────────────────────────────────────

    /** 匹配 {{变量名}} 格式 */
    private static final Pattern VAR = Pattern.compile("\\{\\{(.+?)}}");

    /** 匹配 ${{变量名}} 格式（Nuclei 兼容） */
    private static final Pattern NUCLEI_VAR = Pattern.compile("\\$\\{\\{(.+?)}}");

    /** 匹配 ${:-变量名} 格式（Nuclei 默认值语法） */
    private static final Pattern NUCLEI_DEFAULT = Pattern.compile("\\$\\{:-([^}]+)}");

    // ── 变量存储 ──────────────────────────────────────────────────────

    /** 提取器变量（运行时动态提取，优先级最高） */
    private final Map<String, Object> extractorVars = new HashMap<>();

    /** 模板变量（模板中定义的静态变量） */
    private final Map<String, Object> templateVars;

    // ── 资产信息 ──────────────────────────────────────────────────────

    /** BaseURL：协议://主机:端口（不含路径） */
    private final String baseURL;

    /** 主机名（域名或IP） */
    private final String hostname;

    /** RootURL：协议://主机:端口/（含末尾斜杠） */
    private final String rootURL;

    /**
     * 构造函数 — 从消息中初始化变量上下文。
     *
     * @param msg RocketMQ 消息，包含资产信息和模板变量
     */
    public VariableContext(TaskItemMessage msg) {
        // 初始化模板变量
        this.templateVars = msg.getVariables() != null ? new HashMap<>(msg.getVariables()) : Map.of();

        // 解析资产信息
        String protocol = msg.getAssetProtocol() != null ? msg.getAssetProtocol() : "https";
        String host = msg.getAssetHost() != null ? msg.getAssetHost() : "";
        int port = msg.getAssetPort() != null ? msg.getAssetPort() : ("https".equals(protocol) ? 443 : 80);
        String path = msg.getAssetPath() != null ? msg.getAssetPath() : "/";

        // 判断是否为默认端口（http:80 或 https:443）
        boolean isDefaultPort = ("http".equals(protocol) && port == 80) ||
                               ("https".equals(protocol) && port == 443);
        String portStr = isDefaultPort ? "" : ":" + port;

        // 构建 URL
        this.baseURL = protocol + "://" + host + portStr;
        this.hostname = host;
        this.rootURL = protocol + "://" + host + portStr + "/";
    }

    // ── 变量操作 ──────────────────────────────────────────────────────

    /**
     * 设置提取器变量。
     * <p>
     * 由 ExtractorPipeline 调用，将从 HTTP 响应中提取的值写入上下文。
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void set(String key, Object value) {
        extractorVars.put(key, value);
    }

    /**
     * 获取变量值。
     * <p>
     * 按优先级查找：提取器变量 → 模板变量
     *
     * @param key 变量名
     * @return 变量值，如果不存在返回 null
     */
    public Object get(String key) {
        // 优先查找提取器变量
        Object v = extractorVars.get(key);
        if (v != null) return v;

        // 其次查找模板变量
        v = templateVars.get(key);
        if (v != null) return v;

        return null;
    }

    // ── 变量解析 ──────────────────────────────────────────────────────

    /**
     * 解析模板中的变量占位符。
     * <p>
     * 按顺序解析三种格式：
     * 1. ${{变量名}} — Nuclei 兼容格式
     * 2. ${:-变量名} — Nuclei 默认值语法
     * 3. {{变量名}} — 标准格式
     *
     * @param template 包含变量占位符的模板字符串
     * @return 解析后的字符串
     */
    public String resolve(String template) {
        if (template == null) return null;

        String result = template;

        // 1. 解析 ${{变量名}} 格式（Nuclei 兼容）
        result = resolveNucleiVar(result);

        // 2. 解析 ${:-变量名} 格式（Nuclei 默认值语法）
        result = resolveNucleiDefault(result);

        // 3. 解析标准 {{变量名}} 格式
        result = resolveStandardVar(result);

        return result;
    }

    /**
     * 解析 Nuclei 兼容格式 ${{变量名}}。
     */
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

    /**
     * 解析 Nuclei 默认值语法 ${:-变量名}。
     * <p>
     * 如果变量存在则使用变量值，否则保留原样。
     */
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

    /**
     * 解析标准格式 {{变量名}}。
     */
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

    // ── 内置函数 ──────────────────────────────────────────────────────

    /**
     * 评估内置函数或变量。
     * <p>
     * 支持的内置函数：
     * - BaseURL：协议://主机:端口
     * - Hostname：主机名
     * - RootURL：协议://主机:端口/
     * - rand_int(min,max)：随机整数
     * - rand_base(len)：随机字符串
     * - randstr：随机十六进制字符串
     * - interactsh-url：OAST 交互域名（已由预检器过滤）
     *
     * @param name 函数名或变量名
     * @return 解析后的值
     */
    private String evalBuiltin(String name) {
        return switch (name) {
            // 资产占位符
            case "BaseURL" -> baseURL;
            case "Hostname" -> hostname;
            case "RootURL" -> rootURL;

            // 内置函数
            default -> {
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

    /**
     * 评估 rand_int 函数。
     * <p>
     * 格式：rand_int(min, max) 或 rand_int(max)
     *
     * @param expr 函数表达式
     * @return 随机整数
     */
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

    /**
     * 评估 rand_base 函数。
     * <p>
     * 格式：rand_base(len)
     *
     * @param expr 函数表达式
     * @return 随机字符串
     */
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

    // ── 响应变量更新 ──────────────────────────────────────────────────

    /**
     * 从 HTTP 响应中更新变量。
     * <p>
     * 每次 HTTP 请求完成后调用，将响应信息写入变量上下文，
     * 供后续步骤的提取器和匹配器使用。
     *
     * @param ctx HTTP 响应上下文
     */
    public void updateFrom(HttpResponseContext ctx) {
        set("body", ctx.getBody() != null ? ctx.getBody() : "");
        set("status_code", ctx.getStatusCode());
        set("content_type", ctx.getContentType() != null ? ctx.getContentType() : "");
        set("content_length", ctx.getContentLength());
        set("duration", ctx.getDurationMs());
    }
}

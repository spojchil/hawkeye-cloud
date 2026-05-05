package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpRequestConfig;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.common.util.UrlFixer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP 请求执行器。
 * <p>
 * 负责发送 HTTP 请求并返回响应结果。
 * 使用 Java 11+ 内置 {@link java.net.http.HttpClient}，无需额外依赖。
 * <p>
 * 支持两种请求模式：
 * <ol>
 *   <li><b>普通模式</b>：使用 method + path + headers + body 构建请求</li>
 *   <li><b>Raw 模式</b>：解析原始 HTTP 文本（如 "GET /path HTTP/1.1\nHost: xxx"）</li>
 * </ol>
 * <p>
 * 特性：
 * <ul>
 *   <li>连接池复用：全局共享一个 HttpClient 实例</li>
 *   <li>超时控制：连接超时 5s，请求超时 10s</li>
 *   <li>不跟随重定向：由模板配置决定是否重定向</li>
 *   <li>HTTPS 自动降级：当 HTTPS 失败且错误为 SSLException 时，自动尝试 HTTP</li>
 *   <li>多路径尝试：模板中 path 是数组，逐个尝试直到成功</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *   // 普通模式
 *   HttpResponseContext ctx = httpExecutor.execute(config, vars);
 *
 *   // Raw 模式
 *   HttpResponseContext ctx = httpExecutor.executeRaw(rawText, vars);
 * </pre>
 */
@Slf4j
@Component
public class HttpExecutor {

    // ── 常量 ──────────────────────────────────────────────────────────

    /** 全局 HTTP 客户端（连接池复用） */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** 请求超时时间 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    // ── 普通模式 ──────────────────────────────────────────────────────

    /**
     * 执行 HTTP 请求（普通模式）。
     * <p>
     * 使用 method + path + headers + body 构建请求。
     * 支持多路径尝试：模板中 path 是数组，逐个尝试直到返回非 404/500。
     *
     * @param config   请求配置（已替换变量）
     * @param resolver 变量解析器
     * @return HTTP 响应上下文
     * @throws IOException          IO 异常
     * @throws InterruptedException 线程中断
     */
    public HttpResponseContext execute(HttpRequestConfig config, VariableContext resolver)
            throws IOException, InterruptedException {

        List<String> paths = config.getPaths();
        if (paths == null || paths.isEmpty()) {
            paths = List.of("/");
        }

        // 逐个尝试 path 列表中的路径
        HttpResponseContext lastResponse = null;
        for (String path : paths) {
            String resolvedPath = resolver.resolve(path);
            URI uri = buildUri(resolvedPath);

            // 构建请求
            String method = config.getMethod() != null ? config.getMethod() : "GET";
            HttpRequest.BodyPublisher bodyPublisher = buildBodyPublisher(config, resolver);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .method(method, bodyPublisher);

            // 设置请求头
            setHeaders(builder, config, resolver);

            // 发送请求（带 HTTPS 自动降级）
            long start = System.currentTimeMillis();
            HttpResponse<String> response = sendWithFallback(uri, builder, config, resolver);
            long duration = System.currentTimeMillis() - start;

            // 构建响应上下文
            lastResponse = buildResponseContext(response, duration);

            // 匹配到第一个非 404/500 的就停止
            if (response.statusCode() < 400) {
                return lastResponse;
            }
        }
        return lastResponse;
    }

    // ── Raw 模式 ──────────────────────────────────────────────────────

    /**
     * 执行原始 HTTP 文本请求（Raw 模式）。
     * <p>
     * 解析 raw 文本（如 "GET /path HTTP/1.1\nHost: xxx"），构建并发送。
     * <p>
     * Raw 格式说明：
     * <pre>
     *   GET /api/login HTTP/1.1
     *   Host: {{Hostname}}
     *   Content-Type: application/json
     *
     *   {"username":"admin","password":"123456"}
     * </pre>
     *
     * @param raw      原始 HTTP 文本
     * @param resolver 变量解析器
     * @return HTTP 响应上下文
     * @throws IOException          IO 异常
     * @throws InterruptedException 线程中断
     */
    public HttpResponseContext executeRaw(String raw, VariableContext resolver)
            throws IOException, InterruptedException {

        // 解析变量
        String resolved = resolver.resolve(raw);
        String[] lines = resolved.split("\\r?\\n");
        if (lines.length == 0) {
            throw new IOException("Empty raw request");
        }

        // 解析请求行：METHOD PATH HTTP/1.1
        String[] requestLine = lines[0].split(" ");
        String method = requestLine[0];
        String path = requestLine.length > 1 ? requestLine[1] : "/";

        // 解析 Host 头
        String host = null;
        int bodyStartIndex = 1;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                bodyStartIndex = i + 1;
                break;
            }
            if (line.toLowerCase().startsWith("host:")) {
                host = line.substring(5).trim();
            }
        }

        // 构建完整 URI
        String fullUri = buildFullUri(host, path, resolver);
        URI rawUri = buildUri(fullUri);

        // 构建请求
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(rawUri)
                .timeout(REQUEST_TIMEOUT);

        // 解析 Header 行（跳过 Host，HttpClient 会自动设置）
        int i = 1;
        for (; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) break;
            int colon = line.indexOf(':');
            if (colon > 0) {
                String headerName = line.substring(0, colon).trim();
                String headerValue = line.substring(colon + 1).trim();
                if (!headerName.equalsIgnoreCase("Host")) {
                    builder.header(headerName, headerValue);
                }
            }
        }

        // 解析 Body
        StringBuilder body = new StringBuilder();
        for (i = bodyStartIndex; i < lines.length; i++) {
            if (!body.isEmpty()) body.append("\n");
            body.append(lines[i]);
        }
        HttpRequest.BodyPublisher bodyPublisher = !body.isEmpty()
                ? HttpRequest.BodyPublishers.ofString(body.toString())
                : HttpRequest.BodyPublishers.noBody();
        builder.method(method, bodyPublisher);

        // 发送请求
        long start = System.currentTimeMillis();
        HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        long duration = System.currentTimeMillis() - start;

        return buildResponseContext(response, duration);
    }

    // ── 辅助方法 ──────────────────────────────────────────────────────

    /**
     * 构建 URI。
     * <p>
     * 如果直接创建失败（如包含特殊字符），尝试编码后创建。
     */
    private URI buildUri(String uriStr) throws IOException {
        try {
            return URI.create(uriStr);
        } catch (IllegalArgumentException e) {
            try {
                // 要求uriStr中不能有中文，必须有scheme
                return URI.create(UrlFixer.fix(uriStr));
            } catch (Exception ex) {
                throw new IOException("无法构建 URI: " + uriStr, ex);
            }
        }
    }

    /**
     * 构建完整 URI。
     * <p>
     * 优先使用 Host 头，其次使用 VariableContext 中的 BaseURL。
     */
    private String buildFullUri(String host, String path, VariableContext resolver) {
        if (host != null && !host.isEmpty()) {
            return "http://" + host + path;
        }

        String baseURL = (String) resolver.get("BaseURL");
        if (baseURL != null && !baseURL.isEmpty()) {
            return baseURL + path;
        }

        return "http://localhost" + path;
    }

    /**
     * 构建请求体。
     */
    private HttpRequest.BodyPublisher buildBodyPublisher(HttpRequestConfig config, VariableContext resolver) {
        if (config.getBody() != null && !config.getBody().isEmpty()) {
            String resolvedBody = resolver.resolve(config.getBody());
            return HttpRequest.BodyPublishers.ofString(resolvedBody);
        }
        return HttpRequest.BodyPublishers.noBody();
    }

    /**
     * 设置请求头。
     */
    private void setHeaders(HttpRequest.Builder builder, HttpRequestConfig config, VariableContext resolver) {
        if (config.getHeaders() != null) {
            for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
                builder.header(entry.getKey(), resolver.resolve(entry.getValue()));
            }
        }
    }

    /**
     * 发送请求（带 HTTPS 自动降级）。
     * <p>
     * 如果 HTTPS 请求失败且错误为 SSLException，自动尝试 HTTP 请求。
     * 这解决了资产配置为 HTTPS 但实际服务是 HTTP 的问题。
     */
    private HttpResponse<String> sendWithFallback(URI uri, HttpRequest.Builder builder,
                                                   HttpRequestConfig config, VariableContext resolver)
            throws IOException, InterruptedException {
        try {
            return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (SSLException e) {
            // HTTPS 失败，尝试 HTTP 降级
            if (uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("https")) {
                log.warn("HTTPS 请求失败，尝试 HTTP 降级: {}", e.getMessage());
                URI httpUri = buildHttpUri(uri);
                return sendRequest(httpUri, config.getMethod(), buildBodyPublisher(config, resolver), config, resolver);
            } else {
                throw e;
            }
        }
    }

    /**
     * 发送 HTTP 请求（辅助方法）。
     */
    private HttpResponse<String> sendRequest(URI uri, String method, HttpRequest.BodyPublisher bodyPublisher,
                                              HttpRequestConfig config, VariableContext resolver)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .method(method != null ? method : "GET", bodyPublisher);

        setHeaders(builder, config, resolver);

        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 将 HTTPS URI 转换为 HTTP URI。
     */
    private URI buildHttpUri(URI httpsUri) {
        try {
            return new URI("http", httpsUri.getUserInfo(), httpsUri.getHost(), httpsUri.getPort(),
                    httpsUri.getPath(), httpsUri.getQuery(), httpsUri.getFragment());
        } catch (Exception e) {
            return httpsUri;
        }
    }

    /**
     * 构建响应上下文。
     */
    private HttpResponseContext buildResponseContext(HttpResponse<String> response, long duration) {
        HttpResponseContext ctx = new HttpResponseContext();
        ctx.setStatusCode(response.statusCode());
        ctx.setBody(response.body());
        ctx.setHeaders(response.headers().map());
        ctx.setContentType(response.headers().firstValue("Content-Type").orElse(""));
        ctx.setContentLength(response.body() != null ? response.body().length() : 0);
        ctx.setDurationMs(duration);
        return ctx;
    }
}

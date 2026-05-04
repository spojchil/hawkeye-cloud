package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpRequestConfig;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLException;

/**
 * HTTP 请求执行器。
 * <p>
 * 使用 Java 11+ 内置 {@link java.net.http.HttpClient}，无需额外依赖。
 * 支持 Simple（method + path + headers + body）和 Raw（原始 HTTP 文本）两种模式。
 * 注册为 Spring Bean 以便被 DetectionEngine 注入。
 */
@Slf4j
@Component
public class HttpExecutor {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    /**
     * 执行 HTTP 请求。
     *
     * @param config   请求配置（已替换变量）
     * @param resolver 变量解析器（用于多路径替换后逐个尝试）
     */
    public HttpResponseContext execute(HttpRequestConfig config, VariableContext resolver) throws IOException, InterruptedException {
        List<String> paths = config.getPaths();
        if (paths == null || paths.isEmpty()) {
            paths = List.of("/");
        }

        // 逐个尝试 path 列表中的路径（模板中 path 是数组，可能定义多个候选 URL）
        HttpResponseContext lastResponse = null;
        for (String path : paths) {
            String resolvedPath = resolver.resolve(path);
            URI uri;
            try {
                uri = URI.create(resolvedPath);
            } catch (IllegalArgumentException e) {
                // SQL注入等模板URL含特殊字符（' 等），回退为编码方式
                try {
                    uri = new URI(null, null,
                            java.net.URLEncoder.encode(resolvedPath, java.nio.charset.StandardCharsets.UTF_8),
                            null);
                } catch (Exception ex) {
                    throw new IOException("无法构建 URI: " + resolvedPath, ex);
                }
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT);

            String method = config.getMethod() != null ? config.getMethod() : "GET";
            HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
            if (config.getBody() != null && !config.getBody().isEmpty()) {
                String resolvedBody = resolver.resolve(config.getBody());
                bodyPublisher = HttpRequest.BodyPublishers.ofString(resolvedBody);
            }
            builder.method(method, bodyPublisher);

            // 设置请求头
            if (config.getHeaders() != null) {
                for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
                    builder.header(entry.getKey(), resolver.resolve(entry.getValue()));
                }
            }

            long start = System.currentTimeMillis();
            HttpRequest request = builder.build();
            HttpResponse<String> response;
            try {
                response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (SSLException e) {
                // HTTPS 失败，尝试 HTTP 降级
                if (uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("https")) {
                    log.warn("HTTPS 请求失败，尝试 HTTP 降级: {}", e.getMessage());
                    URI httpUri = buildHttpUri(uri);
                    response = sendRequest(httpUri, method, bodyPublisher, config, resolver);
                } else {
                    throw e;
                }
            }
            long duration = System.currentTimeMillis() - start;

            lastResponse = new HttpResponseContext();
            lastResponse.setStatusCode(response.statusCode());
            lastResponse.setBody(response.body());
            lastResponse.setHeaders(response.headers().map());
            lastResponse.setContentType(response.headers().firstValue("Content-Type").orElse(""));
            lastResponse.setContentLength(response.body() != null ? response.body().length() : 0);
            lastResponse.setDurationMs(duration);

            // path 数组中匹配到第一个非 404/500 的就停止
            if (response.statusCode() < 400) {
                return lastResponse;
            }
        }
        return lastResponse;
    }

    /**
     * 执行原始 HTTP 文本请求。
     * 解析 raw 文本（如 "GET /path HTTP/1.1\nHost: xxx"），构建并发送。
     */
    public HttpResponseContext executeRaw(String raw, VariableContext resolver) throws IOException, InterruptedException {
        String resolved = resolver.resolve(raw);
        String[] lines = resolved.split("\\r?\\n");
        if (lines.length == 0) throw new IOException("Empty raw request");

        // 解析第一行: METHOD PATH HTTP/1.1
        String[] requestLine = lines[0].split(" ");
        String method = requestLine[0];
        String path = requestLine.length > 1 ? requestLine[1] : "/";

        // 解析 Host 头（用于构建完整 URI）
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
        // 如果没有 Host 头，使用 VariableContext 中的 BaseURL
        String fullUri;
        if (host != null && !host.isEmpty()) {
            fullUri = "http://" + host + path;
        } else {
            // 从 VariableContext 获取 BaseURL
            String baseURL = (String) resolver.get("BaseURL");
            if (baseURL != null && !baseURL.isEmpty()) {
                fullUri = baseURL + path;
            } else {
                fullUri = "http://localhost" + path;
            }
        }

        URI rawUri;
        try {
            rawUri = URI.create(fullUri);
        } catch (IllegalArgumentException e) {
            try {
                // 尝试编码路径部分
                URI base = URI.create(fullUri.substring(0, fullUri.indexOf(path)));
                rawUri = new URI(base.getScheme(), base.getHost(), 
                        java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8), 
                        null);
            } catch (Exception ex) {
                throw new IOException("无法构建 URI: " + fullUri, ex);
            }
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(rawUri)
                .timeout(REQUEST_TIMEOUT);

        // 解析 Header 行
        int i = 1;
        for (; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) break;  // 空行 = Header 结束
            int colon = line.indexOf(':');
            if (colon > 0) {
                String headerName = line.substring(0, colon).trim();
                String headerValue = line.substring(colon + 1).trim();
                // 跳过 Host 头（Java HttpClient 会自动设置）
                if (!headerName.equalsIgnoreCase("Host")) {
                    builder.header(headerName, headerValue);
                }
            }
        }

        // 空行之后是 Body
        StringBuilder body = new StringBuilder();
        for (i = bodyStartIndex; i < lines.length; i++) {
            if (body.length() > 0) body.append("\n");
            body.append(lines[i]);
        }
        HttpRequest.BodyPublisher bodyPublisher = body.length() > 0
                ? HttpRequest.BodyPublishers.ofString(body.toString())
                : HttpRequest.BodyPublishers.noBody();
        builder.method(method, bodyPublisher);

        long start = System.currentTimeMillis();
        HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        long duration = System.currentTimeMillis() - start;

        HttpResponseContext ctx = new HttpResponseContext();
        ctx.setStatusCode(response.statusCode());
        ctx.setBody(response.body());
        ctx.setHeaders(response.headers().map());
        ctx.setContentType(response.headers().firstValue("Content-Type").orElse(""));
        ctx.setContentLength(response.body() != null ? response.body().length() : 0);
        ctx.setDurationMs(duration);
        return ctx;
    }

    /**
     * 将 HTTPS URI 转换为 HTTP URI。
     */
    private URI buildHttpUri(URI httpsUri) {
        try {
            return new URI("http", httpsUri.getUserInfo(), httpsUri.getHost(), httpsUri.getPort(),
                    httpsUri.getPath(), httpsUri.getQuery(), httpsUri.getFragment());
        } catch (Exception e) {
            // 如果转换失败，返回原始 URI
            return httpsUri;
        }
    }

    /**
     * 发送 HTTP 请求（带请求头）。
     */
    private HttpResponse<String> sendRequest(URI uri, String method, HttpRequest.BodyPublisher bodyPublisher,
                                              HttpRequestConfig config, VariableContext resolver) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .method(method, bodyPublisher);

        // 设置请求头
        if (config.getHeaders() != null) {
            for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
                builder.header(entry.getKey(), resolver.resolve(entry.getValue()));
            }
        }

        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}

package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpRequestConfig;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import lombok.extern.slf4j.Slf4j;

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
 * 使用 Java 11+ 内置 {@link java.net.http.HttpClient}，无需额外依赖。
 * 支持 Simple（method + path + headers + body）和 Raw（原始 HTTP 文本）两种模式。
 */
@Slf4j
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
    public HttpResponseContext execute(HttpRequestConfig config, VariableResolver resolver) throws IOException, InterruptedException {
        List<String> paths = config.getPaths();
        if (paths == null || paths.isEmpty()) {
            paths = List.of("/");
        }

        // 逐个尝试 path 列表中的路径（模板中 path 是数组，可能定义多个候选 URL）
        HttpResponseContext lastResponse = null;
        for (String path : paths) {
            String resolvedPath = resolver.resolve(path);
            URI uri = URI.create(resolvedPath);

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
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
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
    public HttpResponseContext executeRaw(String raw, VariableResolver resolver) throws IOException, InterruptedException {
        String resolved = resolver.resolve(raw);
        String[] lines = resolved.split("\\r?\\n");
        if (lines.length == 0) throw new IOException("Empty raw request");

        // 解析第一行: METHOD PATH HTTP/1.1
        String[] requestLine = lines[0].split(" ");
        String method = requestLine[0];
        String path = requestLine.length > 1 ? requestLine[1] : "/";

        // 解析后续的 Header 行（直到空行）
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(resolver.resolve(path)))
                .timeout(REQUEST_TIMEOUT);

        int i = 1;
        for (; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) break;  // 空行 = Header 结束
            int colon = line.indexOf(':');
            if (colon > 0) {
                builder.header(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
            }
        }

        // 空行之后是 Body
        StringBuilder body = new StringBuilder();
        for (i = i + 1; i < lines.length; i++) {
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
}

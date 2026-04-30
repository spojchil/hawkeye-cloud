package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage.Extractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExtractorChain 提取器链单元测试")
class ExtractorChainTest {

    private ExtractorChain chain;
    private HttpResponseContext ctx;
    private VariableResolver resolver;

    @BeforeEach
    void setUp() {
        chain = new ExtractorChain();
        ctx = new HttpResponseContext();
        ctx.setBody("{\"token\":\"abc123\",\"user\":\"admin\"}");
        ctx.setStatusCode(200);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Set-Cookie", List.of("JSESSIONID=xyz789; Path=/"));
        headers.put("X-CSRF-TOKEN", List.of("csrf_token_value"));
        ctx.setHeaders(headers);

        resolver = new VariableResolver("https", "example.com", 443, "/", null);
    }

    private static Extractor ext(String type, String part, String name,
                                  Map<String, Object> config, Integer groupNum) {
        Extractor e = new Extractor();
        e.setType(type); e.setPart(part); e.setName(name);
        e.setConfig(config); e.setGroupNum(groupNum);
        return e;
    }

    @Test
    @DisplayName("regex extractor: 从 body 提取")
    void regexExtractBody() {
        chain.extract(ctx, List.of(ext("regex", "body", "token",
                Map.of("regex", List.of("\"token\":\"([^\"]+)\"")), 1)), resolver);
        assertEquals("abc123", resolver.get("token"));
    }

    @Test
    @DisplayName("regex extractor: 从 header 提取")
    void regexExtractHeader() {
        chain.extract(ctx, List.of(ext("regex", "header", "session",
                Map.of("regex", List.of("JSESSIONID=([^;]+)")), 1)), resolver);
        assertEquals("xyz789", resolver.get("session"));
    }

    @Test
    @DisplayName("kval extractor: key-value 提取")
    void kvalExtract() {
        chain.extract(ctx, List.of(ext("kval", null, "csrf",
                Map.of("kval", List.of("X-CSRF-TOKEN")), null)), resolver);
        assertEquals("csrf_token_value", resolver.get("csrf"));
    }

    @Test
    @DisplayName("多个 extractor 同时执行")
    void multipleExtractors() {
        chain.extract(ctx, List.of(
                ext("regex", "body", "token", Map.of("regex", List.of("\"token\":\"([^\"]+)\"")), 1),
                ext("kval", null, "csrf", Map.of("kval", List.of("X-CSRF-TOKEN")), null)
        ), resolver);
        assertEquals("abc123", resolver.get("token"));
        assertEquals("csrf_token_value", resolver.get("csrf"));
    }

    @Test
    @DisplayName("null extractors → 不抛异常")
    void nullExtractors() {
        assertDoesNotThrow(() -> chain.extract(ctx, null, resolver));
    }

    @Test
    @DisplayName("空 extractors → 不抛异常")
    void emptyExtractors() {
        assertDoesNotThrow(() -> chain.extract(ctx, List.of(), resolver));
    }

    @Test
    @DisplayName("不匹配的 regex → 不写入")
    void regexNoMatch() {
        chain.extract(ctx, List.of(ext("regex", "body", "nothing",
                Map.of("regex", List.of("nonexistent")), null)), resolver);
        assertNull(resolver.get("nothing"));
    }
}

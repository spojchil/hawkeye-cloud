package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExtractorChain 提取器链单元测试")
class ExtractorChainTest {

    private ExtractorChain extractorChain;
    private HttpResponseContext ctx;
    private VariableResolver resolver;

    @BeforeEach
    void setUp() {
        extractorChain = new ExtractorChain();
        ctx = new HttpResponseContext();
        ctx.setBody("{\"token\":\"abc123\",\"user\":\"admin\"}");
        ctx.setStatusCode(200);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Set-Cookie", List.of("JSESSIONID=xyz789; Path=/"));
        headers.put("X-CSRF-TOKEN", List.of("csrf_token_value"));
        ctx.setHeaders(headers);

        // dummy resolver
        resolver = new VariableResolver(buildDummyAsset(), buildDummyTemplate());
    }

    private com.hawkeye.detection.common.pojo.dto.AssetDTO buildDummyAsset() {
        var a = new com.hawkeye.detection.common.pojo.dto.AssetDTO();
        a.setRequestProtocol("https");
        a.setRequestHost("example.com");
        return a;
    }

    private com.hawkeye.detection.common.pojo.dto.VulTemplateDTO buildDummyTemplate() {
        var t = new com.hawkeye.detection.common.pojo.dto.VulTemplateDTO();
        t.setVariables("{}");
        return t;
    }

    @Test
    @DisplayName("regex extractor: 从 body 提取 JSON token")
    void regexExtractBody() {
        String extractorsJson = """
            [{"type":"regex","part":"body","name":"token","regex":["\\"token\\":\\"([^\\"]+)\\""],"group":1}]
            """;
        extractorChain.extract(ctx, extractorsJson, resolver);
        assertEquals("abc123", resolver.get("token"));
    }

    @Test
    @DisplayName("regex extractor: 从 header 提取")
    void regexExtractHeader() {
        String extractorsJson = """
            [{"type":"regex","part":"header","name":"session","regex":["JSESSIONID=([^;]+)"],"group":1}]
            """;
        extractorChain.extract(ctx, extractorsJson, resolver);
        assertEquals("xyz789", resolver.get("session"));
    }

    @Test
    @DisplayName("kval extractor: 从 header 提取 Key-Value")
    void kvalExtract() {
        String extractorsJson = """
            [{"type":"kval","name":"csrf","kval":["X-CSRF-TOKEN"]}]
            """;
        extractorChain.extract(ctx, extractorsJson, resolver);
        assertEquals("csrf_token_value", resolver.get("csrf"));
    }

    @Test
    @DisplayName("多个 extractor 同时执行")
    void multipleExtractors() {
        String extractorsJson = """
            [
              {"type":"regex","part":"body","name":"token","regex":["\\"token\\":\\"([^\\"]+)\\""],"group":1},
              {"type":"kval","name":"csrf","kval":["X-CSRF-TOKEN"]}
            ]
            """;
        extractorChain.extract(ctx, extractorsJson, resolver);
        assertEquals("abc123", resolver.get("token"));
        assertEquals("csrf_token_value", resolver.get("csrf"));
    }

    @Test
    @DisplayName("null extractors → 不抛异常")
    void nullExtractors() {
        assertDoesNotThrow(() -> extractorChain.extract(ctx, null, resolver));
    }

    @Test
    @DisplayName("空 extractors → 不抛异常")
    void emptyExtractors() {
        assertDoesNotThrow(() -> extractorChain.extract(ctx, "", resolver));
    }

    @Test
    @DisplayName("不匹配的 regex → 不写入变量")
    void regexNoMatch() {
        String extractorsJson = """
            [{"type":"regex","part":"body","name":"nothing","regex":["nonexistent_pattern_xyz"]}]
            """;
        extractorChain.extract(ctx, extractorsJson, resolver);
        assertNull(resolver.get("nothing"));
    }
}

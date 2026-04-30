package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO.ExtractorDetect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExtractorChain 提取器链单元测试 (v2)")
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
        return t;
    }

    private static ExtractorDetect ext(String type, String part, String name, Map<String, Object> config, Integer groupNum) {
        ExtractorDetect e = new ExtractorDetect();
        e.setType(type);
        e.setPart(part);
        e.setName(name);
        e.setConfig(config);
        e.setGroupNum(groupNum);
        return e;
    }

    @Test
    @DisplayName("regex extractor: 从 body 提取")
    void regexExtractBody() {
        List<ExtractorDetect> list = List.of(
                ext("regex", "body", "token", Map.of("regex", List.of("\"token\":\"([^\"]+)\"")), 1));
        extractorChain.extract(ctx, list, resolver);
        assertEquals("abc123", resolver.get("token"));
    }

    @Test
    @DisplayName("regex extractor: 从 header 提取")
    void regexExtractHeader() {
        List<ExtractorDetect> list = List.of(
                ext("regex", "header", "session", Map.of("regex", List.of("JSESSIONID=([^;]+)")), 1));
        extractorChain.extract(ctx, list, resolver);
        assertEquals("xyz789", resolver.get("session"));
    }

    @Test
    @DisplayName("kval extractor: 从 header 提取 Key-Value")
    void kvalExtract() {
        List<ExtractorDetect> list = List.of(
                ext("kval", null, "csrf", Map.of("kval", List.of("X-CSRF-TOKEN")), null));
        extractorChain.extract(ctx, list, resolver);
        assertEquals("csrf_token_value", resolver.get("csrf"));
    }

    @Test
    @DisplayName("多个 extractor 同时执行")
    void multipleExtractors() {
        List<ExtractorDetect> list = List.of(
                ext("regex", "body", "token", Map.of("regex", List.of("\"token\":\"([^\"]+)\"")), 1),
                ext("kval", null, "csrf", Map.of("kval", List.of("X-CSRF-TOKEN")), null));
        extractorChain.extract(ctx, list, resolver);
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
        assertDoesNotThrow(() -> extractorChain.extract(ctx, List.of(), resolver));
    }

    @Test
    @DisplayName("不匹配的 regex → 不写入变量")
    void regexNoMatch() {
        List<ExtractorDetect> list = List.of(
                ext("regex", "body", "nothing", Map.of("regex", List.of("nonexistent_pattern_xyz")), null));
        extractorChain.extract(ctx, list, resolver);
        assertNull(resolver.get("nothing"));
    }
}

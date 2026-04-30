package com.hawkeye.detection.business.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VariableResolver 变量解析器单元测试")
class VariableResolverTest {

    private VariableResolver buildResolver() {
        return new VariableResolver("https", "example.com", 443, "/api",
                Map.of("email", "test@x.com", "password", "admin"));
    }

    @Test
    @DisplayName("{{BaseURL}} → protocol://host/path")
    void resolveBaseURL() {
        VariableResolver r = buildResolver();
        assertEquals("https://example.com/api", r.resolve("{{BaseURL}}"));
    }

    @Test
    @DisplayName("{{Hostname}} → host")
    void resolveHostname() {
        assertEquals("example.com", buildResolver().resolve("{{Hostname}}"));
    }

    @Test
    @DisplayName("{{RootURL}} → protocol://host")
    void resolveRootURL() {
        assertEquals("https://example.com", buildResolver().resolve("{{RootURL}}"));
    }

    @Test
    @DisplayName("{{Port}} → 443")
    void resolvePort() {
        assertEquals("443", buildResolver().resolve("{{Port}}"));
    }

    @Test
    @DisplayName("{{Path}} → /api")
    void resolvePath() {
        assertEquals("/api", buildResolver().resolve("{{Path}}"));
    }

    @Test
    @DisplayName("模板变量 {{email}}")
    void resolveTemplateVar() {
        assertEquals("test@x.com", buildResolver().resolve("{{email}}"));
    }

    @Test
    @DisplayName("{{randstr}} 8 位随机小写字母")
    void resolveRandStr() {
        String val = buildResolver().resolve("{{randstr}}");
        assertEquals(8, val.length());
        assertTrue(val.matches("[a-z]+"));
    }

    @Test
    @DisplayName("{{rand_base(6)}} 6 位随机")
    void resolveRandBase() {
        String val = buildResolver().resolve("{{rand_base(6)}}");
        assertEquals(6, val.length());
    }

    @Test
    @DisplayName("{{rand_int(100)}} 0~99")
    void resolveRandInt() {
        int n = Integer.parseInt(buildResolver().resolve("{{rand_int(100)}}"));
        assertTrue(n >= 0 && n < 100);
    }

    @Test
    @DisplayName("多个占位符同时替换")
    void resolveMultiple() {
        String result = buildResolver().resolve("{{BaseURL}}/login?user={{email}}");
        assertEquals("https://example.com/api/login?user=test@x.com", result);
    }

    @Test
    @DisplayName("未知占位符 → 空")
    void resolveUnknown() {
        assertEquals("", buildResolver().resolve("{{unknown}}"));
    }

    @Test
    @DisplayName("null → null")
    void resolveNull() {
        assertNull(buildResolver().resolve(null));
    }

    @Test
    @DisplayName("Extractor 变量优先级高于模板变量")
    void extractorVarPriority() {
        VariableResolver r = buildResolver();
        r.put("email", "extracted@y.com");
        assertEquals("extracted@y.com", r.resolve("{{email}}"));
    }

    @Test
    @DisplayName("非标准端口 → URL 带端口")
    void resolveWithPort() {
        VariableResolver r = new VariableResolver("https", "example.com", 8080, "/api", null);
        assertEquals("https://example.com:8080/api", r.resolve("{{BaseURL}}"));
    }

    @Test
    @DisplayName("protocol=null → 默认 https")
    void resolveDefaultProtocol() {
        VariableResolver r = new VariableResolver(null, "example.com", null, "/api", null);
        assertEquals("https://example.com/api", r.resolve("{{BaseURL}}"));
    }

    @Test
    @DisplayName("http:80 → 不追加端口")
    void resolveHttpDefaultPort() {
        VariableResolver r = new VariableResolver("http", "example.com", 80, "/", null);
        assertEquals("http://example.com/", r.resolve("{{BaseURL}}"));
    }
}

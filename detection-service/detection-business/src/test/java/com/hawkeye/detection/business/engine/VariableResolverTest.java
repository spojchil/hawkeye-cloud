package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.common.pojo.dto.AssetDTO;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VariableResolver 变量解析器单元测试")
class VariableResolverTest {

    private AssetDTO buildAsset() {
        AssetDTO asset = new AssetDTO();
        asset.setRequestProtocol("https");
        asset.setRequestHost("example.com");
        asset.setRequestPort(443);
        asset.setRequestPath("/api");
        return asset;
    }

    private VulTemplateDTO buildTemplate() {
        VulTemplateDTO tpl = new VulTemplateDTO();
        tpl.setVariables("{\"email\":\"test@x.com\",\"password\":\"admin\"}");
        return tpl;
    }

    @Test
    @DisplayName("{{BaseURL}} → protocol://host/path")
    void resolveBaseURL() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("https://example.com/api", resolver.resolve("{{BaseURL}}"));
    }

    @Test
    @DisplayName("{{Hostname}} → host")
    void resolveHostname() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("example.com", resolver.resolve("{{Hostname}}"));
    }

    @Test
    @DisplayName("{{RootURL}} → protocol://host")
    void resolveRootURL() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("https://example.com", resolver.resolve("{{RootURL}}"));
    }

    @Test
    @DisplayName("{{Port}} → port")
    void resolvePort() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("443", resolver.resolve("{{Port}}"));
    }

    @Test
    @DisplayName("{{Path}} → asset path")
    void resolvePath() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("/api", resolver.resolve("{{Path}}"));
    }

    @Test
    @DisplayName("模板 variables 字段中定义的变量")
    void resolveTemplateVariable() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("test@x.com", resolver.resolve("{{email}}"));
        assertEquals("admin", resolver.resolve("{{password}}"));
    }

    @Test
    @DisplayName("{{randstr}} → 8 位随机小写字母")
    void resolveRandStr() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        String result = resolver.resolve("{{randstr}}");
        assertEquals(8, result.length());
        assertTrue(result.matches("[a-z]+"));
    }

    @Test
    @DisplayName("{{rand_base(N)}} → N 位随机字母数字")
    void resolveRandBase() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        String result = resolver.resolve("{{rand_base(12)}}");
        assertEquals(12, result.length());
        assertTrue(result.matches("[A-Za-z0-9]+"));
    }

    @Test
    @DisplayName("{{rand_int(N)}} → [0, N) 随机整数")
    void resolveRandInt() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        String result = resolver.resolve("{{rand_int(100)}}");
        int val = Integer.parseInt(result);
        assertTrue(val >= 0 && val < 100);
    }

    @Test
    @DisplayName("混合多个占位符的字符串")
    void resolveMultiple() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        String result = resolver.resolve("{{BaseURL}}/login?user={{email}}");
        assertEquals("https://example.com/api/login?user=test@x.com", result);
    }

    @Test
    @DisplayName("未定义的占位符返回空字符串")
    void resolveUnknown() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("", resolver.resolve("{{nonexistent}}"));
    }

    @Test
    @DisplayName("null 输入返回 null")
    void resolveNull() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        assertNull(resolver.resolve(null));
    }

    @Test
    @DisplayName("Extractor 写入的变量优先级最高")
    void extractorOverridesTemplateVar() {
        VariableResolver resolver = new VariableResolver(buildAsset(), buildTemplate());
        resolver.put("email", "extracted@y.com");
        assertEquals("extracted@y.com", resolver.resolve("{{email}}"));
    }

    @Test
    @DisplayName("带端口的 RootURL（非 80/443）")
    void resolveRootURLWithCustomPort() {
        AssetDTO asset = new AssetDTO();
        asset.setRequestProtocol("http");
        asset.setRequestHost("localhost");
        asset.setRequestPort(8080);
        asset.setRequestPath("/");
        VariableResolver resolver = new VariableResolver(asset, buildTemplate());
        assertEquals("http://localhost:8080", resolver.resolve("{{RootURL}}"));
        assertEquals("http://localhost:8080/", resolver.resolve("{{BaseURL}}"));
    }
}

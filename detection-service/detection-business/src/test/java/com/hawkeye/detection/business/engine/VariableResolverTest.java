package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.common.pojo.dto.AssetDTO;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VariableResolver 变量解析器单元测试 (v2)")
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
        tpl.setVariables(Map.of("email", "test@x.com", "password", "admin"));
        return tpl;
    }

    @Test
    @DisplayName("{{BaseURL}} → protocol://host/path")
    void resolveBaseURL() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("https://example.com/api", r.resolve("{{BaseURL}}"));
    }

    @Test
    @DisplayName("{{Hostname}} → host")
    void resolveHostname() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("example.com", r.resolve("{{Hostname}}"));
    }

    @Test
    @DisplayName("{{RootURL}} → protocol://host")
    void resolveRootURL() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("https://example.com", r.resolve("{{RootURL}}"));
    }

    @Test
    @DisplayName("{{Port}} → 443")
    void resolvePort() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("443", r.resolve("{{Port}}"));
    }

    @Test
    @DisplayName("{{Path}} → /api")
    void resolvePath() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("/api", r.resolve("{{Path}}"));
    }

    @Test
    @DisplayName("模板变量替换：{{email}}")
    void resolveTemplateVar() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("test@x.com", r.resolve("{{email}}"));
    }

    @Test
    @DisplayName("{{randstr}} 生成 8 位随机小写字母")
    void resolveRandStr() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        String val = r.resolve("{{randstr}}");
        assertNotNull(val);
        assertEquals(8, val.length());
        assertTrue(val.matches("[a-z]+"));
    }

    @Test
    @DisplayName("{{rand_base(6)}} 生成 6 位随机字母数字")
    void resolveRandBase() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        String val = r.resolve("{{rand_base(6)}}");
        assertNotNull(val);
        assertEquals(6, val.length());
    }

    @Test
    @DisplayName("{{rand_int(100)}} 生成 0~99 的整数")
    void resolveRandInt() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        String val = r.resolve("{{rand_int(100)}}");
        int n = Integer.parseInt(val);
        assertTrue(n >= 0 && n < 100);
    }

    @Test
    @DisplayName("多个占位符同时替换")
    void resolveMultiple() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        String result = r.resolve("{{BaseURL}}/login?user={{email}}");
        assertEquals("https://example.com/api/login?user=test@x.com", result);
    }

    @Test
    @DisplayName("未知占位符 → 空字符串")
    void resolveUnknown() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        assertEquals("", r.resolve("{{unknown_var}}"));
    }

    @Test
    @DisplayName("null 输入 → null")
    void resolveNull() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        assertNull(r.resolve(null));
    }

    @Test
    @DisplayName("extractor 写入的变量优先级高于模板变量")
    void extractorVarPriority() {
        VariableResolver r = new VariableResolver(buildAsset(), buildTemplate());
        r.put("email", "extracted@y.com");
        assertEquals("extracted@y.com", r.resolve("{{email}}"));
    }

    @Test
    @DisplayName("port≠443 且≠80 → 带端口的 URL")
    void resolveBaseURLWithPort() {
        AssetDTO asset = buildAsset();
        asset.setRequestPort(8080);
        VariableResolver r = new VariableResolver(asset, buildTemplate());
        assertEquals("https://example.com:8080/api", r.resolve("{{BaseURL}}"));
    }

    @Test
    @DisplayName("protocol=null → 默认 https")
    void resolveDefaultProtocol() {
        AssetDTO asset = buildAsset();
        asset.setRequestProtocol(null);
        VariableResolver r = new VariableResolver(asset, buildTemplate());
        assertEquals("https://example.com/api", r.resolve("{{BaseURL}}"));
    }
}

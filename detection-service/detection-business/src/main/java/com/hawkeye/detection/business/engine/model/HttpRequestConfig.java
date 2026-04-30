package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * HTTP 请求配置（从 http_requests JSON 解析）。
 */
@Data
public class HttpRequestConfig {
    private String method;
    private List<String> paths;
    private Map<String, String> headers;
    private String body;
    private String raw;
}

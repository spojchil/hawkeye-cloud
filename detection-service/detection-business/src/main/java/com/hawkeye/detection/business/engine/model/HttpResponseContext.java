package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * HTTP 响应上下文——供 MatcherChain 和 ExtractorChain 使用。
 */
@Data
public class HttpResponseContext {
    private int statusCode;
    private String body;
    private Map<String, List<String>> headers;
    private String contentType;
    private int contentLength;
    private long durationMs;
}

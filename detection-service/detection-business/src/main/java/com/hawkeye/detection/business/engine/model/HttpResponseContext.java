package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * HTTP 响应上下文——封装状态码、body、headers、contentType、耗时，供 Matcher/Extractor 使用
 */
@Data
public class HttpResponseContext {

    /** HTTP 状态码 */
    private int statusCode;

    /** 响应体内容 */
    private String body;

    /** 响应头（键值对，值为列表，因为一个键可以有多个值） */
    private Map<String, List<String>> headers;

    /** Content-Type 响应头 */
    private String contentType;

    /** 响应体长度（字节） */
    private int contentLength;

    /** 请求耗时（毫秒） */
    private long durationMs;
}

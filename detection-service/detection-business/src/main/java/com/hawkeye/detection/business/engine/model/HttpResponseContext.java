package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * HTTP 响应上下文。
 * <p>
 * 封装 HTTP 响应的所有信息，供 Matcher 和 Extractor 使用。
 * <p>
 * 字段说明：
 * <ul>
 *   <li>statusCode - HTTP 状态码（如 200、404）</li>
 *   <li>body - 响应体内容</li>
 *   <li>headers - 响应头（键值对，值为列表）</li>
 *   <li>contentType - Content-Type 响应头</li>
 *   <li>contentLength - 响应体长度</li>
 *   <li>durationMs - 请求耗时（毫秒）</li>
 * </ul>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>Matcher 使用 statusCode、body、headers 进行匹配判定</li>
 *   <li>Extractor 使用 body、headers 提取变量</li>
 *   <li>VariableContext.updateFrom() 将响应信息写入变量上下文</li>
 * </ul>
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

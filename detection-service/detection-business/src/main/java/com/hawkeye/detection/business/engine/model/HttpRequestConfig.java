package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * HTTP 请求配置。
 * <p>
 * 从 TaskItemMessage.HttpStep 转换而来，用于构建 HTTP 请求。
 * <p>
 * 字段说明：
 * <ul>
 *   <li>method - HTTP 方法（GET、POST、PUT 等）</li>
 *   <li>paths - 请求路径列表（支持多路径尝试）</li>
 *   <li>headers - 请求头</li>
 *   <li>body - 请求体</li>
 *   <li>raw - 原始 HTTP 文本（raw 模式使用）</li>
 * </ul>
 * <p>
 * 请求模式：
 * <ul>
 *   <li>普通模式：使用 method + paths + headers + body</li>
 *   <li>Raw 模式：使用 raw（解析原始 HTTP 文本）</li>
 * </ul>
 */
@Data
public class HttpRequestConfig {

    /** HTTP 方法（GET、POST、PUT 等） */
    private String method;

    /** 请求路径列表（支持多路径尝试） */
    private List<String> paths;

    /** 请求头 */
    private Map<String, String> headers;

    /** 请求体 */
    private String body;

    /** 原始 HTTP 文本（raw 模式使用） */
    private String raw;
}

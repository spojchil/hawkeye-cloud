package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * HTTP 请求配置——从 TaskItemMessage.HttpStep 转换而来
 *
 * <p>普通模式用 method+paths+headers+body，Raw 模式用 raw 解析原始 HTTP 文本。</p>
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

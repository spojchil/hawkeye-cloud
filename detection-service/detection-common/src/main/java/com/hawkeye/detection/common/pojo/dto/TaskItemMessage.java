package com.hawkeye.detection.common.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * RocketMQ 消息体 —— task_item 检测组合。
 * <p>
 * 只传 ID 不传全量数据：Worker 通过 Feign 按 ID 拉取资产和模板数据。
 * 5000 条消息只传 500KB，结合缓存后仅需 10 次 Feign 调用。
 */
@Data
public class TaskItemMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long itemId;
    private Long assetId;
    private Long vulId;
    private Long tenantId;
    private Long createdAt;
}

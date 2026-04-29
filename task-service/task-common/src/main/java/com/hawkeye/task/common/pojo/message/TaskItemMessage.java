package com.hawkeye.task.common.pojo.message;

import lombok.Data;

import java.io.Serializable;

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

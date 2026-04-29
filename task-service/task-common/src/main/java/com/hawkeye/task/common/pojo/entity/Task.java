package com.hawkeye.task.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import com.hawkeye.task.common.enums.TaskStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("task")
public class Task extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long taskId;

    private String taskName;

    private String targetIds;

    private String vulIds;

    private TaskStatusEnum status;

    private Integer totalItems;

    private Integer completedItems;

    private Integer failedItems;

    private Integer priority;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String resultSummary;
}

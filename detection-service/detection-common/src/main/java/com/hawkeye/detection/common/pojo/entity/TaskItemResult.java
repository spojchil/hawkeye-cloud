package com.hawkeye.detection.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测项结果（对应 task_item 表，仅含检测引擎需要更新的字段）
 */
@Data
@TableName("task_item")
public class TaskItemResult {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    private Integer status;
    private Integer responseStatusCode;
    private Integer responseSize;
    private String responseSummary;
    private String matchedMatcher;
    private LocalDateTime matchedAt;
    private String errorMessage;
    private Integer durationMs;
}

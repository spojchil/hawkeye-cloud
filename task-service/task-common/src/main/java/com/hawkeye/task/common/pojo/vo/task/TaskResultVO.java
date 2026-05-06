package com.hawkeye.task.common.pojo.vo.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测结果 VO。
 */
@Data
public class TaskResultVO {
    private Long id;
    private Long taskId;
    private Long taskItemId;
    private Long templateId;
    private Long assetId;
    private String status;
    private Integer responseStatusCode;
    private Integer responseSize;
    private String responseSummary;
    private String matchedMatcher;
    private LocalDateTime matchedAt;
    private String errorMessage;
    private Integer durationMs;
}

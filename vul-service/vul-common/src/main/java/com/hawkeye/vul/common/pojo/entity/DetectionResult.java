package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测结果（detection-service 写入的不可变事件日志）。
 * 不继承 BaseEntity——没有 create_by / update_by / deleted 字段。
 */
@Data
@TableName("detection_result")
public class DetectionResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long taskItemId;

    private Long templateId;

    private Long assetId;

    /** matched / not_matched / error */
    private String status;

    private Integer responseStatusCode;

    private Integer responseSize;

    private String responseSummary;

    private String matchedMatcher;

    private LocalDateTime matchedAt;

    private String errorMessage;

    private Integer durationMs;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

package com.hawkeye.detection.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测结果表。
 * <p>
 * 每条 task_item 对应一条检测结果。支持幂等写入（taskItemId 唯一键）。
 * 响应体可能较大，用 MEDIUMTEXT 存储。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("detection_result")
public class DetectionResult extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的任务 ID */
    private Long taskId;

    /** 关联的检测项 ID（唯一键，幂等保障） */
    private Long taskItemId;

    /** 关联的资产 ID */
    private Long assetId;

    /** 关联的漏洞模板 ID */
    private Long vulId;

    /** 执行结果: SUCCESS / FAILED / ERROR */
    private String status;

    /** HTTP 响应体（截断后，最大 64KB） */
    private String responseBody;

    /** HTTP 状态码 */
    private Integer statusCode;

    /** 请求耗时（毫秒） */
    private Long durationMs;

    /** 错误信息（探测失败时） */
    private String errorMessage;

    /** 匹配结果详情（JSON，记录命中了哪些 matcher） */
    private String matchDetail;

    /** 提取器结果（JSON） */
    private String extractResult;
}

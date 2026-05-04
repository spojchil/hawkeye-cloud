package com.hawkeye.detection.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测结果实体。
 * <p>
 * 不可变事件日志，记录每次检测的执行结果。
 * 不继承 BaseEntity（无 create_by/update_by/deleted 字段）。
 * <p>
 * 状态说明：
 * <ul>
 *   <li>matched - 命中漏洞</li>
 *   <li>not_matched - 未命中漏洞</li>
 *   <li>error - 执行异常</li>
 * </ul>
 * <p>
 * 幂等性：
 * <ul>
 *   <li>task_item_id 唯一键保证同一检测项不会重复写入</li>
 * </ul>
 */
@Data
@TableName("detection_result")
public class DetectionResult {

    /** 主键（自增） */
    @TableId(type = IdType.AUTO)
    private Long resultId;

    /** 任务 ID */
    private Long taskId;

    /** 检测项 ID（幂等键） */
    private Long taskItemId;

    /** 模板 ID */
    private Long templateId;

    /** 资产 ID */
    private Long assetId;

    /** 检测状态：matched / not_matched / error */
    private String status;

    /** HTTP 响应状态码 */
    private Integer responseStatusCode;

    /** 响应体大小（字节） */
    private Integer responseSize;

    /** 响应摘要（截断） */
    private String responseSummary;

    /** 命中的匹配器名称 */
    private String matchedMatcher;

    /** 命中时间 */
    private LocalDateTime matchedAt;

    /** 错误信息（status=error 时） */
    private String errorMessage;

    /** 执行耗时（毫秒） */
    private Integer durationMs;

    /** 租户 ID */
    private Long tenantId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

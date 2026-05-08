package com.hawkeye.task.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 检测项——一个资产+一个模板的最小执行单元，合并了原 detection_result 的检测结果字段
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("task_item")
public class TaskItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    /** 所属任务 ID */
    private Long taskId;

    /** 资产 ID */
    private Long assetId;

    /** 漏洞模板 ID（关联 vul_template.template_id） */
    private Long vulId;

    /** 状态 */
    private TaskItemStatusEnum status;

    /* 检测结果字段（原 detection_result 表） */

    /** HTTP 响应状态码 */
    private Integer responseStatusCode;

    /** 响应体大小(字节) */
    private Integer responseSize;

    /** 响应摘要 */
    private String responseSummary;

    /** 命中的匹配器名称 */
    private String matchedMatcher;

    /** 匹配时间 */
    private LocalDateTime matchedAt;

    /** 错误信息 */
    private String errorMessage;

    /** HTTP请求耗时(毫秒) */
    private Integer durationMs;
}

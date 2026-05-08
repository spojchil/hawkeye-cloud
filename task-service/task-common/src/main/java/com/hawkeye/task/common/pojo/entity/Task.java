package com.hawkeye.task.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import com.hawkeye.task.common.enums.TaskStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 检测任务。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("task")
public class Task extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long taskId;

    /** 任务名称 */
    private String taskName;

    /** 资产ID列表（逗号分隔），前端传入 */
    private String targetIds;

    /** 漏洞模板ID列表（逗号分隔），前端传入 */
    private String vulIds;

    /** 状态枚举 */
    private TaskStatusEnum status;

    /* TODO 这里是初始的输入，不是有效的检测项 */
    /** 检测项总数 = 资产数 × 模板数，拆分完成后回填 */
    private Integer totalItems;

    /** 已完成数，TaskProgressScheduler 从 Redis 轮询后回填 */
    private Integer completedItems;

    /** 失败数，同 completedItems 来源 */
    private Integer failedItems;

    /** 优先级，值越小越高，默认 1 */
    private Integer priority;

    /** 异步拆分开始时间（非任务创建时间——任务创建时立刻返回） */
    private LocalDateTime startTime;

    /** 最后一个检测项完成的时间（由 TaskProgressScheduler 标记 DONE 时写入） */
    private LocalDateTime endTime;

    /**
     * 结果摘要 JSON，格式: {@code {"matched":3, "notMatched":46, "error":1}}。
     * TaskProgressScheduler 标记 DONE 时聚合写入。
     */
    private String resultSummary;
}

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

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 目标资产id列表，逗号分割，前端传入
     */
    private String targetIds;

    /**
     * 漏洞模板id列表，逗号分割，前端传入
     */
    private String vulIds;

    /**
     * 任务状态
     */
    private TaskStatusEnum status;

    /**
     * 检测项数
     */
    private Integer totalItems;

    // TODO 这个似乎是要更新？
    /**
     * 已完成数
     */
    private Integer completedItems;

    /**
     * 失败数
     */
    private Integer failedItems;

    /**
     * 优先级，越小越高，默认1
     */
    private Integer priority;

    // TODO 这个开始时间是，前端的任务创建时间吗（立刻返回的）还是说是处理的开始时间
    /**
     * 拆分开始时间
     */
    private LocalDateTime startTime;

    /**
     * 拆分完成时间
     */
    private LocalDateTime endTime;

    // TODO 之后补充结果摘要是什么
    /**
     * 结果摘要
     */
    private String resultSummary;

    // TODO 没有账号id
}

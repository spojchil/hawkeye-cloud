package com.hawkeye.task.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测项——一次任务拆分的最小执行单元（一个资产 + 一个模板）。
 * <p>
 * 数据量大时（M×N 可达数万），需要考虑分库分表（按 task_id 分片）。
 * 检测服务也操作此表，可考虑提取到 common-service。
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

    /** 漏洞模板 ID（关联 vul_template.id） */
    private Long vulId;

    /** PENDING → SUCCESS / NO_MATCH / FAILED */
    private TaskItemStatusEnum status;

    /** 检测结果 JSON，回写 detection-service 执行结果 */
    private String result;
}

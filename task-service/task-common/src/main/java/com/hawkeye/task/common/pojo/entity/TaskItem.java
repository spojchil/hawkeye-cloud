package com.hawkeye.task.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

// TODO 这个如果检测服务也有的话，可以提前到公共服务中
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("task_item")
public class TaskItem extends BaseEntity {

    // TODO 这个进一步的方向是分库分表，一般来说检测项很多
    @TableId(type = IdType.AUTO)
    private Long itemId;

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 资产id
     */
    private Long assetId;

    /**
     * 漏洞模板id
     */
    private Long vulId;

    /**
     * 待执行，成功，未匹配，失败
     */
    private TaskItemStatusEnum status;

    /**
     * 检测结果JSON，
     */
    private String result;
}

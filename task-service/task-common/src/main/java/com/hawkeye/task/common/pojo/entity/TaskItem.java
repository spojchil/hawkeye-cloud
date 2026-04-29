package com.hawkeye.task.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("task_item")
public class TaskItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    private Long taskId;

    private Long assetId;

    private Long vulId;

    private TaskItemStatusEnum status;

    private String result;
}

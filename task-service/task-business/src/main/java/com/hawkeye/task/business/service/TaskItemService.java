package com.hawkeye.task.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.common.pojo.vo.task.TaskItemVO;

public interface TaskItemService extends IService<TaskItem> {

    TaskItemVO.Response updateResult(Long itemId, TaskItemVO.Request request);
}

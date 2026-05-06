package com.hawkeye.task.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.common.pojo.vo.task.TaskItemVO;

/**
 * 检测项服务接口。
 */
public interface TaskItemService extends IService<TaskItem> {

    /** 更新检测结果（detection-service 回写） */
    TaskItemVO.Response updateResult(Long itemId, TaskItemVO.Request request);
}

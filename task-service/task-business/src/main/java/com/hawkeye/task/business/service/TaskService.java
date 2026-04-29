package com.hawkeye.task.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.response.ListResult;
import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;

import java.util.List;

public interface TaskService extends IService<Task> {

    TaskVO.Response create(TaskVO.Request request);

    TaskVO.Response getById(Long taskId);

    ListResult<PageTaskVO.Response> pageQuery(PageTaskVO.Request request);

    void cancel(Long taskId);

    List<Long> listRunningTaskIds();
}

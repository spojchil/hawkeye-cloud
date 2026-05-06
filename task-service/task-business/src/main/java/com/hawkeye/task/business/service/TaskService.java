package com.hawkeye.task.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.response.ListResult;
import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;

import com.hawkeye.task.common.pojo.vo.task.TaskResultVO;

import java.util.List;

/**
 * 任务服务接口。
 */
public interface TaskService extends IService<Task> {

    /** 创建任务（检测拆分+投递） */
    TaskVO.Response create(TaskVO.Request request);

    /** 查询任务详情 */
    TaskVO.Response getById(Long taskId);

    /** 分页查询任务列表 */
    ListResult<PageTaskVO.Response> pageQuery(PageTaskVO.Request request);

    /** 取消任务（仅 PENDING 状态） */
    void cancel(Long taskId);

    /** 查询所有运行中的任务 ID（进度轮询用） */
    List<Long> listRunningTaskIds();

    /** 查询检测结果（分页） */
    ListResult<TaskResultVO> listResults(Long taskId, String status, Integer page, Integer size);
}

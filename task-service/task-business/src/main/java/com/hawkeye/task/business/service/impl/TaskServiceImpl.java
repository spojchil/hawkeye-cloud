package com.hawkeye.task.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.common.utils.response.ListResult;
import com.hawkeye.task.business.mapper.TaskMapper;
import com.hawkeye.task.business.mapstruct.TaskMapstruct;
import com.hawkeye.task.business.service.TaskItemService;
import com.hawkeye.task.business.service.TaskService;
import com.hawkeye.task.common.enums.TaskStatusEnum;
import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private static final int PAGE_SIZE_MAX = 100;

    private final TaskMapstruct taskMapstruct;
    private final TaskItemService taskItemService;

    @Override
    @Transactional
    public TaskVO.Response create(TaskVO.Request request) {
        Task task = taskMapstruct.toEntity(request);
        task.setStatus(TaskStatusEnum.PENDING);
        task.setTotalItems(0);
        task.setCompletedItems(0);
        task.setFailedItems(0);
        if (task.getPriority() == null) {
            task.setPriority(1);
        }
        save(task);
        // ★ 异步线程池负责后续：校验 → 拆分 → 分发 MQ → 标记 RUNNING
        return taskMapstruct.toResponseVO(task);
    }

    @Override
    public TaskVO.Response getById(Long taskId) {
        Task task = lambdaQuery().eq(Task::getTaskId, taskId).one();
        if (task == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "任务不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        return taskMapstruct.toResponseVO(task);
    }

    @Override
    public ListResult<PageTaskVO.Response> pageQuery(PageTaskVO.Request request) {
        int pageSize = Math.min(request.getPageSize() != null ? request.getPageSize() : 10, PAGE_SIZE_MAX);
        int pageNum = request.getPage() != null ? request.getPage() : 1;

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .like(StrUtil.isNotBlank(request.getTaskName()), Task::getTaskName, request.getTaskName())
                .eq(request.getStatus() != null, Task::getStatus, request.getStatus())
                .orderByDesc(Task::getCreateTime);

        Page<Task> page = new Page<>(pageNum, pageSize);
        IPage<Task> result = baseMapper.selectPage(page, wrapper);

        List<PageTaskVO.Response> voList = result.getRecords()
                .stream()
                .map(taskMapstruct::toPageTaskVO)
                .toList();

        return ListResult.result((int) result.getTotal(), voList);
    }

    @Override
    @Transactional
    public void cancel(Long taskId) {
        Task task = lambdaQuery().eq(Task::getTaskId, taskId).one();
        if (task == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "任务不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        if (task.getStatus() != TaskStatusEnum.PENDING) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "仅待执行状态的任务可取消",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }
        lambdaUpdate()
                .eq(Task::getTaskId, taskId)
                .set(Task::getStatus, TaskStatusEnum.CANCELLED)
                .update();
    }

    @Override
    public List<Long> listRunningTaskIds() {
        return lambdaQuery()
                .eq(Task::getStatus, TaskStatusEnum.RUNNING)
                .select(Task::getTaskId)
                .list()
                .stream()
                .map(Task::getTaskId)
                .toList();
    }
}

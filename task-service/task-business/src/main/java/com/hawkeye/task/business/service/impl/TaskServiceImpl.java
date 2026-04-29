package com.hawkeye.task.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
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

import java.util.List;

/**
 * 任务服务实现。
 * <p>
 * 负责任务的创建、查询、分页和取消，对应三层队列架构中的预处理队列入口。
 * 后续异步拆分、Feign 校验、MQ 分发由独立线程池完成（见设计文档）。
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    /** 分页查询每页最大条数，防止恶意请求一次性查询过多数据 */
    private static final int PAGE_SIZE_MAX = 100;

    private final TaskMapstruct taskMapstruct;
    private final TaskItemService taskItemService;

    @LogExecutionTime("创建任务")
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
        return taskMapstruct.toResponseVO(task);
    }

    @LogExecutionTime("查询任务详情")
    @Override
    public TaskVO.Response getById(Long taskId) {
        Task task = baseMapper.selectById(taskId);
        if (task == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "任务不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        return taskMapstruct.toResponseVO(task);
    }

    @LogExecutionTime("任务分页查询")
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

    /**
     * 取消任务。
     * <p>
     * MVP 仅支持 PENDING 状态取消。将状态检查合并到 UPDATE 的 WHERE 条件中，
     * 一条 SQL 原子完成校验+更新，避免先查后改的 TOCTOU 竞态。
     */
    @LogExecutionTime("取消任务")
    @Override
    @Transactional
    public void cancel(Long taskId) {
        boolean updated = lambdaUpdate()
                .eq(Task::getTaskId, taskId)
                .eq(Task::getStatus, TaskStatusEnum.PENDING)
                .set(Task::getStatus, TaskStatusEnum.CANCELLED)
                .update();

        if (!updated) {
            // 更新影响 0 行 → 任务不存在或状态不为 PENDING
            Task task = baseMapper.selectById(taskId);
            if (task == null) {
                throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "任务不存在",
                        HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
            }
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "仅待执行状态的任务可取消",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }
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

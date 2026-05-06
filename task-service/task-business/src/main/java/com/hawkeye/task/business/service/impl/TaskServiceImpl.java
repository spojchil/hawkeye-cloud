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
import com.hawkeye.task.business.mapper.TaskItemMapper;
import com.hawkeye.task.business.mapper.TaskMapper;
import com.hawkeye.task.business.mapstruct.TaskMapstruct;
import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import com.hawkeye.task.common.enums.TaskStatusEnum;
import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.business.service.TaskService;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskResultVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 任务服务实现。
 * <p>
 * 核心职责：创建任务 → 委托 TaskSplitService 异步拆分 → 轮询进度
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private static final int PAGE_SIZE_MAX = 100;

    private final TaskMapstruct taskMapstruct;
    private final TaskItemMapper taskItemMapper;
    private final TaskSplitService taskSplitService;
    private final StringRedisTemplate redisTemplate;

    // ── 创建任务 ──────────────────────────────────────────────────────

    @LogExecutionTime("创建任务")
    @Override
    @Transactional
    public TaskVO.Response create(TaskVO.Request request) {
        // 幂等性检查：同一幂等键返回已有任务
        if (StrUtil.isNotBlank(request.getIdempotencyKey())) {
            String key = "task:idempotent:" + request.getIdempotencyKey();
            String existingTaskId = redisTemplate.opsForValue().get(key);
            if (existingTaskId != null) {
                log.info("幂等请求，返回已有任务 taskId={}", existingTaskId);
                return getById(Long.valueOf(existingTaskId));
            }
        }

        // 1. 构建任务实体
        Task task = taskMapstruct.toEntity(request);
        task.setStatus(TaskStatusEnum.PENDING);
        task.setTotalItems(0);
        task.setCompletedItems(0);
        task.setFailedItems(0);
        if (task.getPriority() == null) task.setPriority(1);

        // 2. 持久化
        save(task);

        // 3. 缓存幂等键 → taskId（1 小时过期）
        if (StrUtil.isNotBlank(request.getIdempotencyKey())) {
            String key = "task:idempotent:" + request.getIdempotencyKey();
            redisTemplate.opsForValue().set(key, String.valueOf(task.getTaskId()), Duration.ofHours(1));
        }

        // 4. 委托异步服务拆分+投递
        taskSplitService.splitAndDispatch(task, request.getAssetIds(), request.getTemplateIds());

        return taskMapstruct.toResponseVO(task);
    }

    // ── 查询接口 ──────────────────────────────────────────────────────

    @LogExecutionTime("查询任务详情")
    @Override
    public TaskVO.Response getById(Long taskId) {
        Task task = getTaskOrThrow(taskId);
        TaskVO.Response response = taskMapstruct.toResponseVO(task);

        // 查询检测项列表
        List<TaskItem> items = taskItemMapper.selectList(
                new LambdaQueryWrapper<TaskItem>()
                        .eq(TaskItem::getTaskId, taskId)
                        .eq(TaskItem::getDeletedAt, 0L)
                        .orderByAsc(TaskItem::getItemId));

        // 转换为 TaskItemDetail
        List<TaskVO.Response.TaskItemDetail> itemDetails = items.stream()
                .map(this::toTaskItemDetail)
                .toList();
        response.setItems(itemDetails);

        return response;
    }

    @LogExecutionTime("任务分页查询")
    @Override
    public ListResult<PageTaskVO.Response> pageQuery(PageTaskVO.Request request) {
        int pageSize = Math.min(request.getPageSize() != null ? request.getPageSize() : 10, PAGE_SIZE_MAX);
        int pageNum = request.getPage() != null ? request.getPage() : 1;

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getDeletedAt, 0L)
                .like(StrUtil.isNotBlank(request.getTaskName()), Task::getTaskName, request.getTaskName())
                .eq(request.getStatus() != null, Task::getStatus, request.getStatus())
                .orderByDesc(Task::getCreateTime);

        Page<Task> page = new Page<>(pageNum, pageSize);
        IPage<Task> result = baseMapper.selectPage(page, wrapper);

        List<PageTaskVO.Response> voList = result.getRecords().stream()
                .map(taskMapstruct::toPageTaskVO)
                .toList();

        return ListResult.result((int) result.getTotal(), voList);
    }

    @LogExecutionTime("取消任务")
    @Override
    @Transactional
    public void cancel(Long taskId) {
        // 原子更新：只有 PENDING 状态才能取消
        boolean updated = lambdaUpdate()
                .eq(Task::getTaskId, taskId)
                .eq(Task::getDeletedAt, 0L)
                .eq(Task::getStatus, TaskStatusEnum.PENDING)
                .set(Task::getStatus, TaskStatusEnum.CANCELLED)
                .update();

        if (!updated) {
            Task task = baseMapper.selectOne(
                    new LambdaQueryWrapper<Task>()
                            .eq(Task::getTaskId, taskId)
                            .eq(Task::getDeletedAt, 0L));
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
                .eq(Task::getDeletedAt, 0L)
                .eq(Task::getStatus, TaskStatusEnum.RUNNING)
                .select(Task::getTaskId)
                .list()
                .stream()
                .map(Task::getTaskId)
                .toList();
    }

    @Override
    @LogExecutionTime("查询检测结果")
    public ListResult<TaskResultVO> listResults(Long taskId, String status, Integer page, Integer size) {
        getTaskOrThrow(taskId);

        // 从 task_item 表查询结果
        LambdaQueryWrapper<TaskItem> wrapper = new LambdaQueryWrapper<TaskItem>()
                .eq(TaskItem::getTaskId, taskId)
                .eq(TaskItem::getDeletedAt, 0L)
                .eq(StrUtil.isNotBlank(status), TaskItem::getStatus, parseStatus(status))
                .orderByAsc(TaskItem::getItemId);

        int pageSize = Math.min(size != null ? size : 20, 200);
        int pageNum = page != null ? Math.max(page, 1) : 1;
        Page<TaskItem> pg = new Page<>(pageNum, pageSize);
        IPage<TaskItem> result = taskItemMapper.selectPage(pg, wrapper);

        List<TaskResultVO> vos = result.getRecords().stream()
                .map(this::toTaskResultVO)
                .toList();

        return ListResult.result((int) result.getTotal(), vos);
    }

    // ── 工具方法 ──────────────────────────────────────────────────────

    /** 解析状态字符串为枚举 */
    private TaskItemStatusEnum parseStatus(String status) {
        if (status == null) return null;
        return switch (status.toLowerCase()) {
            case "matched" -> TaskItemStatusEnum.MATCHED;
            case "not_matched" -> TaskItemStatusEnum.NOT_MATCHED;
            case "error" -> TaskItemStatusEnum.FAILED;
            case "skipped" -> TaskItemStatusEnum.SKIPPED;
            default -> null;
        };
    }

    /** 转换 TaskItem 为 TaskItemDetail */
    private TaskVO.Response.TaskItemDetail toTaskItemDetail(TaskItem item) {
        TaskVO.Response.TaskItemDetail detail = new TaskVO.Response.TaskItemDetail();
        detail.setItemId(item.getItemId());
        detail.setAssetId(item.getAssetId());
        detail.setVulId(item.getVulId());
        detail.setStatus(item.getStatus() != null ? item.getStatus().getDescription() : "待执行");
        detail.setResponseStatusCode(item.getResponseStatusCode());
        detail.setDurationMs(item.getDurationMs());
        detail.setMatchedMatcher(item.getMatchedMatcher());
        detail.setErrorMessage(item.getErrorMessage());
        return detail;
    }

    /** 转换检测结果为 VO */
    private TaskResultVO toTaskResultVO(TaskItem item) {
        TaskResultVO vo = new TaskResultVO();
        vo.setId(item.getItemId());
        vo.setTaskId(item.getTaskId());
        vo.setTaskItemId(item.getItemId());
        vo.setTemplateId(item.getVulId());
        vo.setAssetId(item.getAssetId());
        vo.setStatus(item.getStatus() != null ? item.getStatus().getDescription() : "-");
        vo.setResponseStatusCode(item.getResponseStatusCode());
        vo.setResponseSize(item.getResponseSize());
        vo.setResponseSummary(item.getResponseSummary());
        vo.setMatchedMatcher(item.getMatchedMatcher());
        vo.setMatchedAt(item.getMatchedAt());
        vo.setErrorMessage(item.getErrorMessage());
        vo.setDurationMs(item.getDurationMs());
        return vo;
    }

    /** 获取任务或抛出异常 */
    private Task getTaskOrThrow(Long taskId) {
        Task task = baseMapper.selectOne(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getTaskId, taskId)
                        .eq(Task::getDeletedAt, 0L));
        if (task == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "任务不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        return task;
    }
}

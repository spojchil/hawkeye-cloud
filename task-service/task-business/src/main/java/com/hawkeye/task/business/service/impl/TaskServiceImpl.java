package com.hawkeye.task.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.common.utils.response.ListResult;
import com.hawkeye.task.common.pojo.dto.AssetBrief;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
import com.hawkeye.task.business.cache.TemplateCache;
import com.hawkeye.task.business.feign.AssetServiceFeign;
import com.hawkeye.task.business.mapper.DetectionResultMapper;
import com.hawkeye.task.business.mapper.TaskItemMapper;
import com.hawkeye.task.business.mapper.TaskMapper;
import com.hawkeye.task.business.mapstruct.TaskMapstruct;
import com.hawkeye.task.business.mq.TaskProducerService;
import com.hawkeye.task.business.service.TaskItemPreChecker;
import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import com.hawkeye.task.common.enums.TaskStatusEnum;
import com.hawkeye.task.common.pojo.dto.TemplateDetectConfig;
import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.business.service.TaskService;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskResultVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private static final int PAGE_SIZE_MAX = 100;
    private static final int BATCH_INSERT_SIZE = 1000;

    private final TaskMapstruct taskMapstruct;
    private final TaskItemMapper taskItemMapper;
    private final TemplateCache templateCache;
    private final AssetServiceFeign assetServiceFeign;
    private final TaskProducerService taskProducerService;
    private final DetectionResultMapper detectionResultMapper;
    private final TaskItemPreChecker preChecker;

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

        splitAndDispatch(task, request.getAssetIds(), request.getTemplateIds());

        return taskMapstruct.toResponseVO(task);
    }

    @Async("taskSplitExecutor")
    public void splitAndDispatch(Task task, List<Long> assetIds, List<Long> templateIds) {
        try {
            doSplitAndDispatch(task, assetIds, templateIds);
        } catch (Exception e) {
            log.error("任务拆分失败 taskId={}", task.getTaskId(), e);
            markTaskError(task, e.getMessage());
        }
    }

    @Transactional
    protected void doSplitAndDispatch(Task task, List<Long> assetIds, List<Long> templateIds) {
        task.setStatus(TaskStatusEnum.RUNNING);
        task.setStartTime(LocalDateTime.now());
        updateById(task);

        Map<Long, TemplateDetectConfig> templates = templateCache.batchGet(templateIds);
        if (templates.isEmpty()) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "无有效模板",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        Map<Long, AssetBrief> assets = fetchAssetsWithLog(assetIds);
        if (assets.isEmpty()) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "无有效资产",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        List<TaskItem> validItems = buildTaskItems(task, assetIds, templateIds, assets, templates);

        if (validItems.isEmpty()) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "预检后无有效检测项",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        batchInsertTaskItems(validItems);

        task.setTotalItems(validItems.size());
        updateById(task);

        List<TaskItemMessage> messages = buildMessages(task, validItems, assets, templates);
        taskProducerService.sendBatch(messages, this::markItemFailed);
    }

    private void batchInsertTaskItems(List<TaskItem> items) {
        for (int i = 0; i < items.size(); i += BATCH_INSERT_SIZE) {
            int end = Math.min(i + BATCH_INSERT_SIZE, items.size());
            List<TaskItem> batch = items.subList(i, end);
            for (TaskItem item : batch) {
                taskItemMapper.insert(item);
            }
        }
    }

    private Map<Long, AssetBrief> fetchAssetsWithLog(List<Long> assetIds) {
        Map<Long, AssetBrief> assets = new LinkedHashMap<>();
        List<Long> failedIds = new ArrayList<>();

        for (Long assetId : assetIds) {
            try {
                var resp = assetServiceFeign.getAsset(assetId);
                if (resp != null && resp.getData() != null) {
                    assets.put(assetId, resp.getData());
                } else {
                    failedIds.add(assetId);
                }
            } catch (Exception e) {
                log.warn("拉取资产失败 assetId={}: {}", assetId, e.getMessage());
                failedIds.add(assetId);
            }
        }

        if (!failedIds.isEmpty()) {
            log.warn("部分资产拉取失败: {}", failedIds);
        }

        return assets;
    }

    private List<TaskItem> buildTaskItems(Task task, List<Long> assetIds, List<Long> templateIds,
                                          Map<Long, AssetBrief> assets,
                                          Map<Long, TemplateDetectConfig> templates) {
        List<TaskItem> items = new ArrayList<>();
        int skippedCount = 0;

        for (Long assetId : assetIds) {
            AssetBrief asset = assets.get(assetId);
            if (asset == null) continue;

            for (Long templateId : templateIds) {
                TemplateDetectConfig tpl = templates.get(templateId);
                if (tpl == null) continue;

                // 预检：过滤掉不支持的模板（如使用 payloads 的模板）
                if (!preChecker.preCheck(asset, tpl)) {
                    skippedCount++;
                    continue;
                }

                TaskItem item = new TaskItem();
                item.setTaskId(task.getTaskId());
                item.setAssetId(assetId);
                item.setVulId(templateId);
                item.setStatus(TaskItemStatusEnum.PENDING);
                items.add(item);
            }
        }

        if (skippedCount > 0) {
            log.info("预检剔除 {} 个无效检测项（可能包含payloads模板）", skippedCount);
        }

        return items;
    }

    private List<TaskItemMessage> buildMessages(Task task, List<TaskItem> items,
                                                Map<Long, AssetBrief> assets,
                                                Map<Long, TemplateDetectConfig> templates) {
        List<TaskItemMessage> messages = new ArrayList<>();
        long now = System.currentTimeMillis();
        Long tenantId = parseTenantId();

        for (TaskItem item : items) {
            AssetBrief asset = assets.get(item.getAssetId());
            TemplateDetectConfig tpl = templates.get(item.getVulId());
            if (asset == null || tpl == null) continue;

            TaskItemMessage msg = new TaskItemMessage();
            msg.setTaskId(task.getTaskId());
            msg.setItemId(item.getItemId());
            msg.setAssetId(item.getAssetId());
            msg.setTenantId(tenantId);
            msg.setCreatedAt(now);
            msg.setAssetProtocol(asset.getRequestProtocol());
            msg.setAssetHost(asset.getRequestHost());
            msg.setAssetPort(asset.getRequestPort());
            msg.setAssetPath(asset.getRequestPath() != null ? asset.getRequestPath() : "/");
            msg.setTemplateId(tpl.getYamlId());
            msg.setTemplateDbId(item.getVulId());
            msg.setFlow(tpl.getFlow());
            msg.setVariables(tpl.getVariables());
            msg.setHttpSteps(toHttpSteps(tpl.getHttpSteps()));
            messages.add(msg);
        }

        return messages;
    }

    private List<TaskItemMessage.HttpStep> toHttpSteps(List<TemplateDetectConfig.HttpStepConfig> steps) {
        if (steps == null) return null;

        return steps.stream().map(s -> {
            TaskItemMessage.HttpStep hs = new TaskItemMessage.HttpStep();
            hs.setStepOrder(s.getStepOrder());
            hs.setMethod(s.getMethod());
            hs.setPath(s.getPath());
            hs.setHeaders(s.getHeaders());
            hs.setBody(s.getBody());
            hs.setRaw(s.getRaw());
            hs.setAttack(s.getAttack());
            hs.setMatchersCondition(s.getMatchersCondition());

            if (s.getMatchers() != null) {
                hs.setMatchers(s.getMatchers().stream().map(m -> {
                    TaskItemMessage.Matcher hm = new TaskItemMessage.Matcher();
                    hm.setType(m.getType());
                    hm.setPart(m.getPart());
                    hm.setCondition(m.getInnerCondition());
                    hm.setNegative(m.getNegative());
                    hm.setCaseInsensitive(m.getCaseInsensitive());
                    hm.setConfig(m.getConfig());
                    return hm;
                }).toList());
            }

            if (s.getExtractors() != null) {
                hs.setExtractors(s.getExtractors().stream().map(e -> {
                    TaskItemMessage.Extractor he = new TaskItemMessage.Extractor();
                    he.setType(e.getType());
                    he.setPart(e.getPart());
                    he.setName(e.getExtractorName());
                    he.setConfig(e.getConfig());
                    he.setInternal(e.getInternal());
                    he.setGroupNum(e.getGroupNum());
                    return he;
                }).toList());
            }

            return hs;
        }).toList();
    }

    private void markTaskError(Task task, String message) {
        task.setStatus(TaskStatusEnum.ERROR);
        task.setResultSummary("{\"error\":\"" + message + "\"}");
        updateById(task);
    }

    private void markItemFailed(Long itemId) {
        taskItemMapper.update(null,
                new LambdaUpdateWrapper<TaskItem>()
                        .eq(TaskItem::getItemId, itemId)
                        .set(TaskItem::getStatus, TaskItemStatusEnum.FAILED));
    }

    @LogExecutionTime("查询任务详情")
    @Override
    public TaskVO.Response getById(Long taskId) {
        Task task = getTaskOrThrow(taskId);
        return taskMapstruct.toResponseVO(task);
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

        LambdaQueryWrapper<DetectionResult> wrapper = new LambdaQueryWrapper<DetectionResult>()
                .eq(DetectionResult::getTaskId, taskId)
                .eq(StrUtil.isNotBlank(status), DetectionResult::getStatus, status)
                .orderByAsc(DetectionResult::getTaskItemId);

        int pageSize = Math.min(size != null ? size : 20, 200);
        int pageNum = page != null ? Math.max(page, 1) : 1;
        Page<DetectionResult> pg = new Page<>(pageNum, pageSize);
        IPage<DetectionResult> result = detectionResultMapper.selectPage(pg, wrapper);

        List<TaskResultVO> vos = result.getRecords().stream()
                .map(this::toTaskResultVO)
                .toList();

        return ListResult.result((int) result.getTotal(), vos);
    }

    private TaskResultVO toTaskResultVO(DetectionResult r) {
        TaskResultVO vo = new TaskResultVO();
        vo.setId(r.getResultId());
        vo.setTaskId(r.getTaskId());
        vo.setTaskItemId(r.getTaskItemId());
        vo.setTemplateId(r.getTemplateId());
        vo.setAssetId(r.getAssetId());
        vo.setStatus(r.getStatus());
        vo.setResponseStatusCode(r.getResponseStatusCode());
        vo.setResponseSize(r.getResponseSize());
        vo.setResponseSummary(r.getResponseSummary());
        vo.setMatchedMatcher(r.getMatchedMatcher());
        vo.setMatchedAt(r.getMatchedAt());
        vo.setErrorMessage(r.getErrorMessage());
        vo.setDurationMs(r.getDurationMs());
        return vo;
    }

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

    private static Long parseTenantId() {
        String tid = RequestContext.getHeader(HeaderConstants.HEADER_TENANT_ID);
        if (tid == null || tid.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(tid);
        } catch (NumberFormatException e) {
            log.warn("无效的租户ID: {}", tid);
            return 0L;
        }
    }
}

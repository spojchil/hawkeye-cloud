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
import com.hawkeye.task.business.cache.TemplateCache;
import com.hawkeye.task.business.feign.AssetServiceFeign;
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

/**
 * 任务服务实现。
 * <p>
 * 核心职责：创建任务 → 异步拆分 → 投递 MQ → 轮询进度
 */
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
    private final TaskItemPreChecker preChecker;

    // ── 创建任务 ──────────────────────────────────────────────────────

    @LogExecutionTime("创建任务")
    @Override
    @Transactional
    public TaskVO.Response create(TaskVO.Request request) {
        // 1. 构建任务实体
        Task task = taskMapstruct.toEntity(request);
        task.setStatus(TaskStatusEnum.PENDING);
        task.setTotalItems(0);
        task.setCompletedItems(0);
        task.setFailedItems(0);
        if (task.getPriority() == null) task.setPriority(1);

        // 2. 持久化
        save(task);

        // 3. 异步拆分+投递（新线程）
        splitAndDispatch(task, request.getAssetIds(), request.getTemplateIds());

        return taskMapstruct.toResponseVO(task);
    }

    // ── 异步拆分 ──────────────────────────────────────────────────────

    /** 异步执行拆分，失败时标记任务为 ERROR */
    @Async("taskSplitExecutor")
    public void splitAndDispatch(Task task, List<Long> assetIds, List<Long> templateIds) {
        try {
            doSplitAndDispatch(task, assetIds, templateIds);
        } catch (Exception e) {
            log.error("任务拆分失败 taskId={}", task.getTaskId(), e);
            markTaskError(task, e.getMessage());
        }
    }

    /** 拆分核心逻辑：获取模板 → 获取资产 → 预检 → 批量插入 → 投递 MQ */
    @Transactional
    protected void doSplitAndDispatch(Task task, List<Long> assetIds, List<Long> templateIds) {
        // 更新状态为执行中
        task.setStatus(TaskStatusEnum.RUNNING);
        task.setStartTime(LocalDateTime.now());
        updateById(task);

        // 批量获取模板配置（带缓存）
        Map<Long, TemplateDetectConfig> templates = templateCache.batchGet(templateIds);
        if (templates.isEmpty()) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "无有效模板",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        // 批量获取资产信息
        Map<Long, AssetBrief> assets = fetchAssetsWithLog(assetIds);
        if (assets.isEmpty()) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "无有效资产",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        // 构建检测项（含预检）
        List<TaskItem> validItems = buildTaskItems(task, assetIds, templateIds, assets, templates);
        if (validItems.isEmpty()) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "预检后无有效检测项",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        // 批量插入检测项
        batchInsertTaskItems(validItems);
        task.setTotalItems(validItems.size());
        updateById(task);

        // 构建并发送 MQ 消息
        List<TaskItemMessage> messages = buildMessages(task, validItems, assets, templates);
        taskProducerService.sendBatch(messages, this::markItemFailed);
    }

    // ── 批量操作 ──────────────────────────────────────────────────────

    /** 分批插入检测项 */
    private void batchInsertTaskItems(List<TaskItem> items) {
        for (int i = 0; i < items.size(); i += BATCH_INSERT_SIZE) {
            int end = Math.min(i + BATCH_INSERT_SIZE, items.size());
            List<TaskItem> batch = items.subList(i, end);
            for (TaskItem item : batch) {
                taskItemMapper.insert(item);
            }
        }
    }

    // ── 资产获取 ──────────────────────────────────────────────────────

    /** 批量获取资产，失败的资产记录日志但不中断 */
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

    // ── 检测项构建 ────────────────────────────────────────────────────

    /** 构建检测项列表，预检过滤无效项 */
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

                // 预检：过滤 payloads、OAST 等不支持的模板
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
            log.info("预检剔除 {} 个无效检测项", skippedCount);
        }
        return items;
    }

    // ── 消息构建 ──────────────────────────────────────────────────────

    /** 构建 MQ 消息列表 */
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
            // 任务标识
            msg.setTaskId(task.getTaskId());
            msg.setItemId(item.getItemId());
            msg.setAssetId(item.getAssetId());
            msg.setTenantId(tenantId);
            msg.setCreatedAt(now);
            // 资产信息
            msg.setAssetProtocol(asset.getRequestProtocol());
            msg.setAssetHost(asset.getRequestHost());
            msg.setAssetPort(asset.getRequestPort());
            msg.setAssetPath(asset.getRequestPath() != null ? asset.getRequestPath() : "/");
            // 模板配置
            msg.setTemplateId(tpl.getYamlId());
            msg.setTemplateDbId(item.getVulId());
            msg.setFlow(tpl.getFlow());
            msg.setVariables(tpl.getVariables());
            msg.setHttpSteps(toHttpSteps(tpl.getHttpSteps()));
            messages.add(msg);
        }
        return messages;
    }

    /** 转换 HTTP 步骤配置为消息格式 */
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

            // 转换匹配器
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

            // 转换提取器
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

    // ── 状态标记 ──────────────────────────────────────────────────────

    /** 标记任务为异常状态 */
    private void markTaskError(Task task, String message) {
        task.setStatus(TaskStatusEnum.ERROR);
        task.setResultSummary("{\"error\":\"" + message + "\"}");
        updateById(task);
    }

    /** 标记检测项为失败状态（MQ 发送失败回调） */
    private void markItemFailed(Long itemId) {
        taskItemMapper.update(null,
                new LambdaUpdateWrapper<TaskItem>()
                        .eq(TaskItem::getItemId, itemId)
                        .set(TaskItem::getStatus, TaskItemStatusEnum.FAILED));
    }

    // ── 查询接口 ──────────────────────────────────────────────────────

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

    // ── 工具方法 ──────────────────────────────────────────────────────

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

    /** 解析租户ID，无效时返回 0 */
    private static Long parseTenantId() {
        String tid = RequestContext.getHeader(HeaderConstants.HEADER_TENANT_ID);
        if (tid == null || tid.isBlank()) return 0L;
        try {
            return Long.parseLong(tid);
        } catch (NumberFormatException e) {
            log.warn("无效的租户ID: {}", tid);
            return 0L;
        }
    }
}

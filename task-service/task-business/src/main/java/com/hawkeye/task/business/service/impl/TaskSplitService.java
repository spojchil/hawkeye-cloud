package com.hawkeye.task.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import com.hawkeye.task.business.cache.TemplateCache;
import com.hawkeye.task.business.feign.AssetServiceFeign;
import com.hawkeye.task.business.mapper.TaskItemMapper;
import com.hawkeye.task.business.mapper.TaskMapper;
import com.hawkeye.task.business.mq.TaskProducerService;
import com.hawkeye.task.business.service.TaskItemPreChecker;
import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import com.hawkeye.task.common.enums.TaskStatusEnum;
import com.hawkeye.task.common.pojo.dto.AssetBrief;
import com.hawkeye.task.common.pojo.dto.TemplateDetectConfig;
import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务拆分服务（独立类，解决 @Async 自调用问题）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSplitService extends ServiceImpl<TaskMapper, Task> {

    private static final int BATCH_INSERT_SIZE = 1000;

    private final TaskMapper taskMapper;
    private final TaskItemMapper taskItemMapper;
    private final TemplateCache templateCache;
    private final AssetServiceFeign assetServiceFeign;
    private final TaskProducerService taskProducerService;
    private final TaskItemPreChecker preChecker;

    /**
     * 异步拆分并投递任务。
     */
    @Async("taskSplitExecutor")
    public void splitAndDispatch(Task task, List<Long> assetIds, List<Long> templateIds) {
        try {
            // TODO 事务失效
            doSplitAndDispatch(task, assetIds, templateIds);
        } catch (Exception e) {
            // TODO 有自定义的错误类，
            log.error("任务拆分失败 taskId={}", task.getTaskId(), e);
            markTaskError(task, e.getMessage());
            // TODO 没有重新抛出
        }
    }

    /**
     * 拆分核心逻辑。
     */
    @Transactional
    protected void doSplitAndDispatch(Task task, List<Long> assetIds, List<Long> templateIds) {
        // 更新状态为执行中
        new LambdaUpdateWrapper<Task>()
                .eq(Task::getTaskId, task.getTaskId())
                .set(Task::getStatus, TaskStatusEnum.RUNNING)
                .set(Task::getStartTime, LocalDateTime.now());

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
        taskMapper.updateById(task);

        // 构建并发送 MQ 消息
        List<TaskItemMessage> messages = buildMessages(task, validItems, assets, templates);
        taskProducerService.sendBatch(messages, this::markItemFailed);
    }

    /**
     * 批量插入检测项。
     */
    private void batchInsertTaskItems(List<TaskItem> items) {
        for (int i = 0; i < items.size(); i += BATCH_INSERT_SIZE) {
            int end = Math.min(i + BATCH_INSERT_SIZE, items.size());
            List<TaskItem> batch = items.subList(i, end);
            for (TaskItem item : batch) {
                taskItemMapper.insert(item);
            }
        }
    }

    /**
     * 批量获取资产，失败的资产记录日志但不中断。
     */
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

    /**
     * 构建检测项列表，预检过滤无效项。
     */
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

    /**
     * 构建 MQ 消息列表。
     */
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

    /**
     * 转换 HTTP 步骤配置为消息格式。
     */
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

    /**
     * 标记任务为异常状态。
     */
    private void markTaskError(Task task, String message) {
        task.setStatus(TaskStatusEnum.ERROR);
        task.setResultSummary("{\"error\":\"" + message + "\"}");
        taskMapper.updateById(task);
    }

    /**
     * 标记检测项为失败状态（MQ 发送失败回调）。
     */
    private void markItemFailed(Long itemId) {
        taskItemMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TaskItem>()
                        .eq(TaskItem::getItemId, itemId)
                        .set(TaskItem::getStatus, TaskItemStatusEnum.FAILED));
    }

    /**
     * 解析租户ID。
     */
    private static Long parseTenantId() {
        String tid = RequestContext.getHeader(HeaderConstants.HEADER_TENANT_ID);
        if (tid == null || tid.isBlank()) return 0L;
        try {
            return Long.parseLong(tid);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}

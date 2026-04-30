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
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import com.hawkeye.task.business.cache.TemplateCache;
import com.hawkeye.task.business.feign.AssetServiceFeign;
import com.hawkeye.task.business.mapper.TaskItemMapper;
import com.hawkeye.task.business.mapper.TaskMapper;
import com.hawkeye.task.business.mapstruct.TaskMapstruct;
import com.hawkeye.task.business.service.TaskItemService;
import com.hawkeye.task.business.service.TaskService;
import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import com.hawkeye.task.common.enums.TaskStatusEnum;
import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;
import com.hawkeye.task.business.mq.TaskProducerService;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private static final int PAGE_SIZE_MAX = 100;

    private final TaskMapstruct taskMapstruct;
    private final TaskItemService taskItemService;
    private final TaskItemMapper taskItemMapper;
    private final TemplateCache templateCache;
    private final AssetServiceFeign assetServiceFeign;
    private final TaskProducerService taskProducerService;

    @LogExecutionTime("创建任务")
    @Override
    @Transactional
    public TaskVO.Response create(TaskVO.Request request) {
        Task task = taskMapstruct.toEntity(request);
        task.setStatus(TaskStatusEnum.PENDING);
        task.setTotalItems(0);
        task.setCompletedItems(0);
        task.setFailedItems(0);
        if (task.getPriority() == null) task.setPriority(1);
        save(task);
        splitAndDispatch(task, request.getAssetIds(), request.getTemplateIds());
        return taskMapstruct.toResponseVO(task);
    }

    @Async
    @Transactional
    public void splitAndDispatch(Task task, List<Long> assetIds, List<Long> templateIds) {
        try {
            task.setStatus(TaskStatusEnum.RUNNING);
            updateById(task);

            // 批量拉取模板
            Map<Long, VulTemplateDetectDTO> templates = templateCache.batchGet(templateIds);
            if (templates.isEmpty()) {
                task.setStatus(TaskStatusEnum.ERROR);
                updateById(task);
                return;
            }

            // 批量拉取资产
            Map<Long, Map<String, Object>> assets = new LinkedHashMap<>();
            for (Long assetId : assetIds) {
                try {
                    var resp = assetServiceFeign.getAsset(assetId);
                    if (resp != null && resp.getData() != null) {
                        assets.put(assetId, resp.getData());
                    }
                } catch (Exception e) {
                    log.warn("拉取资产失败 assetId={}: {}", assetId, e.getMessage());
                }
            }
            if (assets.isEmpty()) {
                task.setStatus(TaskStatusEnum.ERROR);
                updateById(task);
                return;
            }

            // 拆分 M×N
            int total = assetIds.size() * templates.size();
            task.setTotalItems(total);
            updateById(task);

            long now = System.currentTimeMillis();
            List<TaskItemMessage> messages = new ArrayList<>();
            List<Long> failedItemIds = new ArrayList<>();

            for (Long assetId : assetIds) {
                Map<String, Object> asset = assets.get(assetId);
                if (asset == null) continue;

                String protocol = (String) asset.getOrDefault("requestProtocol", "https");
                String host = (String) asset.getOrDefault("requestHost", "");
                Integer port = asset.get("requestPort") instanceof Number n
                        ? n.intValue() : 443;
                String path = (String) asset.getOrDefault("requestPath", "/");

                for (Long templateId : templateIds) {
                    VulTemplateDetectDTO tpl = templates.get(templateId);
                    if (tpl == null) continue;

                    // 持久化 task_item
                    TaskItem item = new TaskItem();
                    item.setTaskId(task.getTaskId());
                    item.setAssetId(assetId);
                    item.setVulId(templateId);
                    item.setStatus(TaskItemStatusEnum.PENDING);
                    taskItemMapper.insert(item);

                    // 构建消息
                    TaskItemMessage msg = new TaskItemMessage();
                    msg.setTaskId(task.getTaskId());
                    msg.setItemId(item.getItemId());
                    msg.setTenantId(1L);
                    msg.setCreatedAt(now);
                    msg.setAssetProtocol(protocol);
                    msg.setAssetHost(host);
                    msg.setAssetPort(port);
                    msg.setAssetPath(path);
                    msg.setTemplateId(tpl.getTemplateId());
                    msg.setFlow(tpl.getFlow());
                    msg.setVariables(tpl.getVariables());
                    msg.setHttpSteps(tpl.getHttpSteps() != null
                            ? tpl.getHttpSteps().stream().map(s -> {
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
                                        hm.setType(m.getType()); hm.setPart(m.getPart());
                                        hm.setCondition(m.getCondition());
                                        hm.setNegative(m.getNegative());
                                        hm.setCaseInsensitive(m.getCaseInsensitive());
                                        hm.setConfig(m.getConfig());
                                        return hm;
                                    }).toList());
                                }
                                if (s.getExtractors() != null) {
                                    hs.setExtractors(s.getExtractors().stream().map(e -> {
                                        TaskItemMessage.Extractor he = new TaskItemMessage.Extractor();
                                        he.setType(e.getType()); he.setPart(e.getPart());
                                        he.setName(e.getName()); he.setConfig(e.getConfig());
                                        he.setInternal(e.getInternal());
                                        he.setGroupNum(e.getGroupNum());
                                        return he;
                                    }).toList());
                                }
                                return hs;
                            }).toList() : null);

                    messages.add(msg);
                }
            }

            // 投递消息队列
            if (!messages.isEmpty()) {
                taskProducerService.sendAsync(messages, failedMsg -> {
                    taskItemMapper.update(null,
                            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TaskItem>()
                                    .eq(TaskItem::getItemId, failedMsg.getItemId())
                                    .set(TaskItem::getStatus, TaskItemStatusEnum.FAILED));
                });
            }
        } catch (Exception e) {
            log.error("任务拆分异常 taskId={}: {}", task.getTaskId(), e.getMessage(), e);
            task.setStatus(TaskStatusEnum.ERROR);
            updateById(task);
        }
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

        List<PageTaskVO.Response> voList = result.getRecords().stream()
                .map(taskMapstruct::toPageTaskVO).toList();

        return ListResult.result((int) result.getTotal(), voList);
    }

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
        return lambdaQuery().eq(Task::getStatus, TaskStatusEnum.RUNNING)
                .select(Task::getTaskId).list()
                .stream().map(Task::getTaskId).toList();
    }
}

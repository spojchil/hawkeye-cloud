package com.hawkeye.detection.business.consumer;

import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import com.hawkeye.detection.business.engine.DetectionEngine;
import com.hawkeye.detection.business.mapper.TaskItemResultMapper;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import com.hawkeye.detection.common.pojo.entity.TaskItemResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 检测任务消息消费者
 *
 * <p>CLUSTERING 集群模式，64 线程消费。重试 16 次后进入 %DLQ% 死信。
 * 消费前查 task_item 状态实现幂等，从消息中提取 tenantId 设置租户上下文。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "task_item_topic",
        consumerGroup = "task_item_consumer_group",
        consumeThreadMax = 64)
public class TaskItemConsumer implements RocketMQListener<TaskItemMessage> {

    private final DetectionEngine detectionEngine;
    private final TaskItemResultMapper taskItemResultMapper;

    /**
     * 消费消息。
     *
     * @param message 检测任务消息
     */
    @Override
    @LogExecutionTime
    public void onMessage(TaskItemMessage message) {
        log.debug("收到检测消息: taskId={}, itemId={}, templateId={}, host={}",
                message.getTaskId(), message.getItemId(),
                message.getTemplateId(), message.getAssetHost());

        if (message.getItemId() == null) {
            log.warn("消息缺少 itemId，无法处理 taskId={}", message.getTaskId());
            return;
        }

        if (isAlreadyProcessed(message.getItemId())) {
            log.info("检测项已处理，跳过 itemId={}", message.getItemId());
            return;
        }

        if (message.getTenantId() != null) {
            RequestContext.setHeaders(Map.of(
                    HeaderConstants.HEADER_TENANT_ID, String.valueOf(message.getTenantId())));
        }

        try {
            detectionEngine.execute(message);
        } finally {
            RequestContext.clear();
        }
    }

    private boolean isAlreadyProcessed(Long itemId) {
        TaskItemResult existing = taskItemResultMapper.selectById(itemId);
        if (existing == null || existing.getStatus() == null) {
            return false;
        }
        return existing.getStatus() != 0;
    }
}

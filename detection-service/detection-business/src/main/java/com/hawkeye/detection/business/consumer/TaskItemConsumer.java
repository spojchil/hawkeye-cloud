package com.hawkeye.detection.business.consumer;

import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import com.hawkeye.detection.business.engine.DetectionEngine;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 检测组合消息消费者。
 * <p>
 * 每个 detection-service 实例通过 CLUSTERING 模式竞争消费 task_item 消息。
 * 消息内容只含 ID（taskId / itemId / assetId / vulId），
 * 实际资产和模板数据由 DetectionEngine 内部通过 Feign + 缓存获取。
 * <p>
 * <b>重试策略：</b> 消费抛异常 → RocketMQ 自动重投递（默认 16 次 + 递增延迟），
 * 耗竭后转入 %DLQ% 死信队列。数据缺失类错误不应抛异常（避免无意义重试）。
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

    @Override
    @LogExecutionTime
    public void onMessage(TaskItemMessage message) {
        log.debug("收到检测消息: taskId={}, itemId={}, templateId={}, host={}",
                message.getTaskId(), message.getItemId(),
                message.getTemplateId(), message.getAssetHost());

        // ★ ThreadLocal 在当前单线程消费场景下安全。
        //    如果后续引入虚拟线程并发执行 detectionEngine.execute()，
        //    需将 tenantId 显式传递给新线程，或使用 InheritableThreadLocal。
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
}

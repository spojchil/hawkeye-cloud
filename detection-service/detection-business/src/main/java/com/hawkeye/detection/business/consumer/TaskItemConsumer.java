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
 * 检测任务消息消费者。
 * <p>
 * 从 RocketMQ 消费检测任务消息，调用 DetectionEngine 执行检测。
 * <p>
 * 消费模式：
 * <ul>
 *   <li>CLUSTERING - 集群模式，多个实例竞争消费</li>
 *   <li>最大线程数 - 64（支持高并发）</li>
 * </ul>
 * <p>
 * 重试策略：
 * <ul>
 *   <li>消费抛异常 → RocketMQ 自动重投递（默认 16 次 + 递增延迟）</li>
 *   <li>耗竭后转入 %DLQ% 死信队列</li>
 *   <li>数据缺失类错误不应抛异常（避免无意义重试）</li>
 * </ul>
 * <p>
 * 租户上下文：
 * <ul>
 *   <li>从消息中提取 tenantId，设置到 RequestContext</li>
 *   <li>用于多租户数据隔离</li>
 *   <li>注意：ThreadLocal 在当前单线程消费场景下安全</li>
 * </ul>
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

        // 设置租户上下文
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

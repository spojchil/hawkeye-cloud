package com.hawkeye.detection.business.consumer;

import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 检测组合消息消费者。
 * <p>
 * 每个 detection-service 实例通过 CLUSTERING 模式竞争消费 task_item 消息。
 * 消息内容只含 ID（taskId / itemId / assetId / vulId），
 * 实际资产和模板数据由 Worker 内部通过 Feign + 缓存获取。
 * <p>
 * <b>重试策略：</b> 消费抛异常 → RocketMQ 自动重投递（默认 16 次 + 递增延迟），
 * 耗竭后转入 %DLQ% 死信队列。数据缺失类错误不应抛异常（避免无意义重试）。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "task_item_topic",
        consumerGroup = "task_item_consumer_group",
        consumeThreadMax = 64)
public class TaskItemConsumer implements RocketMQListener<TaskItemMessage> {

    @Override
    public void onMessage(TaskItemMessage message) {
        log.info("收到检测消息: taskId={}, itemId={}, assetId={}, vulId={}",
                message.getTaskId(), message.getItemId(),
                message.getAssetId(), message.getVulId());

        // TODO: 实现检测引擎调用
        // 1. TemplateFetcher 获取模板（Caffeine → Redis → Feign vul-service）
        // 2. AssetFetcher 获取资产（Feign asset-service）
        // 3. VariableResolver 构建上下文
        // 4. FlowExecutor 解析执行计划
        // 5. HttpExecutor 发送 HTTP 请求
        // 6. ExtractorChain 提取变量
        // 7. MatcherChain 匹配判定
        // 8. ResultWriter 批量写 DB + Redis INCR

        log.info("检测完成（占位）: taskId={}, itemId={}", message.getTaskId(), message.getItemId());
    }
}

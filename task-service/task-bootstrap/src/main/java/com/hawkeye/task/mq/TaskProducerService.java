package com.hawkeye.task.mq;

import com.alibaba.fastjson2.JSON;
import com.hawkeye.task.common.pojo.message.TaskItemMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * RocketMQ Producer：将 task_item 消息发送到工作队列。
 * <p>
 * MVP 阶段使用 asyncSend 逐条发送，由 RocketMQ 自行负载均衡到 32 个 Queue。
 * 后续阶段（P1）引入 LoadAwareMessageQueueSelector 实现基于 Redis Worker 负载的定向分发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProducerService {

    private final RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "task_item_topic";
    private static final long SEND_TIMEOUT = 3000L;

    /**
     * 逐条异步发送 task_item 消息。
     *
     * @param items        待发送的 item 消息列表
     * @param onSendFailed 单条发送失败时的回调（标记 item 为 FAILED）
     */
    public void sendAsync(List<TaskItemMessage> items, Consumer<TaskItemMessage> onSendFailed) {
        for (TaskItemMessage item : items) {
            Message<String> msg = MessageBuilder
                    .withPayload(JSON.toJSONString(item))
                    .build();

            rocketMQTemplate.asyncSend(TOPIC, msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult result) {
                    log.debug("MQ 发送成功: taskId={}, itemId={}, msgId={}",
                            item.getTaskId(), item.getItemId(), result.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("MQ 发送失败: taskId={}, itemId={}", item.getTaskId(), item.getItemId(), e);
                    onSendFailed.accept(item);
                }
            }, SEND_TIMEOUT);
        }
    }
}

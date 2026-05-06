package com.hawkeye.task.business.mq;

import com.alibaba.fastjson2.JSON;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
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
 * RocketMQ Producer：投递包含全量执行数据的 task_item 消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProducerService {

    private final RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "task_item_topic";
    private static final long SEND_TIMEOUT = 3000L;

    /**
     * 批量发送消息。
     *
     * @param items        消息列表
     * @param onSendFailed 发送失败回调（传递 itemId）
     */
    public void sendBatch(List<TaskItemMessage> items, Consumer<Long> onSendFailed) {
        for (TaskItemMessage item : items) {
            sendAsync(item, onSendFailed);
        }
    }

    /**
     * 异步发送单条消息。
     *
     * @param item         消息
     * @param onSendFailed 发送失败回调（传递 itemId）
     */
    public void sendAsync(TaskItemMessage item, Consumer<Long> onSendFailed) {
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
                onSendFailed.accept(item.getItemId());
            }
        }, SEND_TIMEOUT);
    }

    /**
     * @deprecated 使用 {@link #sendBatch(List, Consumer)} 替代
     */
    @Deprecated
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

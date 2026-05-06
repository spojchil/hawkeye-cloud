package com.hawkeye.detection.business.engine;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hawkeye.detection.business.mapper.TaskItemResultMapper;
import com.hawkeye.detection.common.enums.DetectionStatusEnum;
import com.hawkeye.detection.common.pojo.entity.TaskItemResult;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 检测结果批量写入器。
 * <p>
 * 更新 task_item 表的检测结果字段，同时更新 Redis 计数。
 * <p>
 * 设计要点：
 * <ol>
 *   <li><b>批量缓冲</b>：使用 ConcurrentLinkedQueue 缓冲检测结果</li>
 *   <li><b>高效计数</b>：使用 AtomicInteger 计数器</li>
 *   <li><b>定时刷新</b>：每 5 秒自动刷新缓冲区</li>
 *   <li><b>阈值触发</b>：缓冲区达到 500 条时立即触发写入</li>
 *   <li><b>优雅关闭</b>：@PreDestroy 钩子确保应用关闭时数据不丢失</li>
 * </ol>
 */
@Slf4j
@Component
public class ResultWriter {

    /** 刷新阈值 */
    private static final int FLUSH_SIZE = 500;

    /** 结果缓冲区 */
    private final Queue<DetectionResultUpdate> buffer = new ConcurrentLinkedQueue<>();

    /** 缓冲区计数器 */
    private final AtomicInteger counter = new AtomicInteger(0);

    private final TaskItemResultMapper taskItemResultMapper;
    private final StringRedisTemplate redisTemplate;

    public ResultWriter(TaskItemResultMapper taskItemResultMapper, StringRedisTemplate redisTemplate) {
        this.taskItemResultMapper = taskItemResultMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 写入检测结果。
     */
    public void write(DetectionResultUpdate result) {
        buffer.add(result);
        int count = counter.incrementAndGet();

        // 更新 Redis 计数
        updateRedisCounter(result);

        // 达到阈值时触发写入
        if (count >= FLUSH_SIZE) {
            flush();
        }
    }

    /**
     * 定时刷新（每 5 秒）。
     */
    @Scheduled(fixedDelay = 5000)
    public void flushByTimeout() {
        flush();
    }

    /**
     * 批量更新 task_item 表。
     */
    private synchronized void flush() {
        int count = Math.min(counter.get(), FLUSH_SIZE);
        if (count == 0) return;

        List<DetectionResultUpdate> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            DetectionResultUpdate r = buffer.poll();
            if (r == null) break;
            batch.add(r);
        }

        if (batch.isEmpty()) return;

        counter.addAndGet(-batch.size());

        // 逐条更新 task_item 表
        for (DetectionResultUpdate result : batch) {
            try {
                // 将状态字符串转换为整数
                Integer statusCode = switch (result.status()) {
                    case "matched" -> 1;
                    case "not_matched" -> 2;
                    case "error" -> 3;
                    default -> 0;
                };

                LambdaUpdateWrapper<TaskItemResult> wrapper = new LambdaUpdateWrapper<TaskItemResult>()
                        .eq(TaskItemResult::getItemId, result.taskItemId())
                        .set(TaskItemResult::getStatus, statusCode)
                        .set(TaskItemResult::getResponseStatusCode, result.responseStatusCode())
                        .set(TaskItemResult::getResponseSize, result.responseSize())
                        .set(TaskItemResult::getResponseSummary, result.responseSummary())
                        .set(TaskItemResult::getMatchedMatcher, result.matchedMatcher())
                        .set(TaskItemResult::getMatchedAt, result.matchedAt())
                        .set(TaskItemResult::getErrorMessage, result.errorMessage())
                        .set(TaskItemResult::getDurationMs, result.durationMs());
                taskItemResultMapper.update(null, wrapper);
            } catch (Exception e) {
                log.error("更新 task_item 失败: itemId={}", result.taskItemId(), e);
            }
        }

        log.debug("批量更新 {} 条检测结果", batch.size());
    }

    /**
     * 更新 Redis 计数（幂等）。
     * <p>
     * 使用 SETNX 确保同一 itemId 只计数一次，防止重复消费导致计数膨胀。
     * key 格式：task:{taskId}:{status}，TTL 24 小时。
     */
    private void updateRedisCounter(DetectionResultUpdate result) {
        try {
            String processedKey = "task:processed:" + result.taskItemId();
            Boolean firstTime = redisTemplate.opsForValue()
                    .setIfAbsent(processedKey, "1", java.time.Duration.ofHours(24));
            if (Boolean.TRUE.equals(firstTime)) {
                String counterKey = "task:" + result.taskId() + ":" + result.status();
                redisTemplate.opsForValue().increment(counterKey);
                redisTemplate.expire(counterKey, java.time.Duration.ofHours(24));
            }
        } catch (Exception e) {
            log.warn("Redis 计数更新失败 taskId={}, itemId={}: {}", result.taskId(), result.taskItemId(), e.getMessage());
        }
    }

    /**
     * 应用关闭时刷入剩余数据。
     */
    @PreDestroy
    public void onDestroy() {
        int remaining = counter.get();
        if (remaining > 0) {
            log.info("应用关闭，刷入剩余 {} 条数据", remaining);
            flush();
        }
    }

    /**
     * 检测结果更新记录。
     */
    public record DetectionResultUpdate(
            Long taskId,
            Long taskItemId,
            String status,       // matched / not_matched / error
            Integer responseStatusCode,
            Integer responseSize,
            String responseSummary,
            String matchedMatcher,
            java.time.LocalDateTime matchedAt,
            String errorMessage,
            Integer durationMs
    ) {
    }
}

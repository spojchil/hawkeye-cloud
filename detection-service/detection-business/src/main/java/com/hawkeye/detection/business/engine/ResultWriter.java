package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.mapper.DetectionResultMapper;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
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
 * 设计要点：
 * 1. 使用 ConcurrentLinkedQueue 替代 synchronized + ArrayList
 * 2. 使用 AtomicInteger 计数器（ConcurrentLinkedQueue.size() 是 O(n)）
 * 3. @PreDestroy 钩子确保应用关闭时数据不丢失
 * 4. flush 方法使用 synchronized 防止并发写入
 */
@Slf4j
@Component
public class ResultWriter {

    private static final int FLUSH_SIZE = 500;

    private final Queue<DetectionResult> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger counter = new AtomicInteger(0);

    private final DetectionResultMapper mapper;
    private final StringRedisTemplate redisTemplate;

    public ResultWriter(DetectionResultMapper mapper, StringRedisTemplate redisTemplate) {
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 写入单条检测结果。
     * <p>
     * 先写入本地缓冲区，同时更新 Redis 计数。
     * 当缓冲区达到阈值时触发批量写入。
     */
    public void write(DetectionResult result) {
        buffer.add(result);
        int count = counter.incrementAndGet();

        // 更新 Redis 计数
        try {
            redisTemplate.opsForValue().increment(
                    "task:" + result.getTaskId() + ":" + result.getStatus());
        } catch (Exception e) {
            log.warn("Redis INCR 失败 taskId={}: {}", result.getTaskId(), e.getMessage());
        }

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
     * 批量写入数据库。
     * <p>
     * 使用 synchronized 防止多个线程同时写入。
     */
    private synchronized void flush() {
        int count = Math.min(counter.get(), FLUSH_SIZE);
        if (count == 0) return;

        List<DetectionResult> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            DetectionResult r = buffer.poll();
            if (r == null) break;
            batch.add(r);
        }

        if (!batch.isEmpty()) {
            counter.addAndGet(-batch.size());
            try {
                mapper.insert(batch);
                log.debug("批量写入 {} 条检测结果", batch.size());
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                // 主键冲突：记录日志，不重试（避免无限循环）
                if (errorMsg != null && errorMsg.contains("Duplicate entry")) {
                    log.warn("批量写入 {} 条检测结果，部分主键冲突已忽略", batch.size());
                } else {
                    // 其他异常：记录错误日志，不放回 buffer（避免无限重试）
                    log.error("批量写入检测结果失败，数据已丢失: {}", errorMsg, e);
                }
            }
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
}

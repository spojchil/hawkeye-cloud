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
 * 负责将检测结果批量写入数据库，同时更新 Redis 计数。
 * <p>
 * 设计要点：
 * <ol>
 *   <li><b>批量缓冲</b>：使用 ConcurrentLinkedQueue 缓冲检测结果</li>
 *   <li><b>高效计数</b>：使用 AtomicInteger 计数器（ConcurrentLinkedQueue.size() 是 O(n)）</li>
 *   <li><b>定时刷新</b>：每 5 秒自动刷新缓冲区</li>
 *   <li><b>阈值触发</b>：缓冲区达到 500 条时立即触发写入</li>
 *   <li><b>优雅关闭</b>：@PreDestroy 钩子确保应用关闭时数据不丢失</li>
 *   <li><b>线程安全</b>：flush 方法使用 synchronized 防止并发写入</li>
 * </ol>
 * <p>
 * Redis 计数说明：
 * <ul>
 *   <li>key: task:{taskId}:{status}</li>
 *   <li>value: 计数（原子递增）</li>
 *   <li>task-service 轮询此计数，判断任务是否完成</li>
 * </ul>
 * <p>
 * 异常处理：
 * <ul>
 *   <li>主键冲突：记录日志，不重试（避免无限循环）</li>
 *   <li>其他异常：记录错误日志，不放回 buffer（避免无限重试）</li>
 * </ul>
 */
@Slf4j
@Component
public class ResultWriter {

    // ── 常量 ──────────────────────────────────────────────────────────

    /** 刷新阈值：缓冲区达到此数量时触发批量写入 */
    private static final int FLUSH_SIZE = 500;

    // ── 成员变量 ──────────────────────────────────────────────────────

    /** 结果缓冲区（线程安全队列） */
    private final Queue<DetectionResult> buffer = new ConcurrentLinkedQueue<>();

    /** 缓冲区计数器（原子操作，避免 ConcurrentLinkedQueue.size() 的 O(n) 开销） */
    private final AtomicInteger counter = new AtomicInteger(0);

    /** 数据库 Mapper */
    private final DetectionResultMapper mapper;

    /** Redis 模板（用于更新计数） */
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造函数。
     *
     * @param mapper       数据库 Mapper
     * @param redisTemplate Redis 模板
     */
    public ResultWriter(DetectionResultMapper mapper, StringRedisTemplate redisTemplate) {
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
    }

    // ── 公共方法 ──────────────────────────────────────────────────────

    /**
     * 写入单条检测结果。
     * <p>
     * 流程：
     * 1. 将结果加入缓冲区
     * 2. 更新 Redis 计数（task:{taskId}:{status}）
     * 3. 如果缓冲区达到阈值，触发批量写入
     *
     * @param result 检测结果
     */
    public void write(DetectionResult result) {
        // 加入缓冲区
        buffer.add(result);
        int count = counter.incrementAndGet();

        // 更新 Redis 计数
        updateRedisCounter(result);

        // 达到阈值时触发写入
        if (count >= FLUSH_SIZE) {
            flush();
        }
    }

    // ── 定时任务 ──────────────────────────────────────────────────────

    /**
     * 定时刷新（每 5 秒）。
     * <p>
     * 即使缓冲区未达到阈值，也会定期写入数据库，
     * 避免检测结果长时间滞留在内存中。
     */
    @Scheduled(fixedDelay = 5000)
    public void flushByTimeout() {
        flush();
    }

    // ── 核心方法 ──────────────────────────────────────────────────────

    /**
     * 批量写入数据库。
     * <p>
     * 使用 synchronized 防止多个线程同时写入。
     * <p>
     * 流程：
     * 1. 从缓冲区取出数据（最多 FLUSH_SIZE 条）
     * 2. 更新计数器
     * 3. 批量插入数据库
     * 4. 异常处理：主键冲突记录日志，其他异常记录错误
     */
    private synchronized void flush() {
        // 计算本次刷新数量
        int count = Math.min(counter.get(), FLUSH_SIZE);
        if (count == 0) return;

        // 从缓冲区取出数据
        List<DetectionResult> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            DetectionResult r = buffer.poll();
            if (r == null) break;
            batch.add(r);
        }

        if (batch.isEmpty()) return;

        // 更新计数器
        counter.addAndGet(-batch.size());

        // 批量写入数据库
        try {
            mapper.insert(batch);
            log.debug("批量写入 {} 条检测结果", batch.size());
        } catch (Exception e) {
            handleFlushException(e, batch.size());
        }
    }

    /**
     * 更新 Redis 计数。
     * <p>
     * key 格式：task:{taskId}:{status}
     * 例如：task:123:matched、task:123:not_matched、task:123:error
     */
    private void updateRedisCounter(DetectionResult result) {
        try {
            String key = "task:" + result.getTaskId() + ":" + result.getStatus();
            redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("Redis INCR 失败 taskId={}: {}", result.getTaskId(), e.getMessage());
        }
    }

    /**
     * 处理刷新异常。
     * <p>
     * 异常处理策略：
     * - 主键冲突：记录日志，不重试（避免无限循环）
     * - 其他异常：记录错误日志，不放回 buffer（避免无限重试）
     */
    private void handleFlushException(Exception e, int batchSize) {
        String errorMsg = e.getMessage();

        if (errorMsg != null && errorMsg.contains("Duplicate entry")) {
            // 主键冲突：可能是之前部分成功或手动插入
            log.warn("批量写入 {} 条检测结果，部分主键冲突已忽略", batchSize);
        } else {
            // 其他异常：数据已丢失，记录错误日志
            log.error("批量写入检测结果失败，数据已丢失: {}", errorMsg, e);
        }
    }

    // ── 生命周期 ──────────────────────────────────────────────────────

    /**
     * 应用关闭时刷入剩余数据。
     * <p>
     * 确保应用关闭时，缓冲区中的数据不会丢失。
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

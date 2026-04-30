package com.hawkeye.detection.business.engine;

import com.common.utils.annotation.LogExecutionTime;
import com.hawkeye.detection.business.mapper.DetectionResultMapper;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测结果批量写入器。
 * <p>
 * 检测结果不逐条 INSERT，先写入本地缓冲区。
 * 缓冲区满 500 条或距上次 flush 超过 5s 时，批量写入 DB。
 * 每条结果完成后立即 INCR Redis 计数，供 task-service 轮询进度。
 * <p>
 * ★ 所有写方法为 synchronized，因为 write() 由 RocketMQ 多线程回调，
 *   flushByTimeout() 由 @Scheduled 定时任务触发，共享同一个 buffer。
 */
@Slf4j
@Component
public class ResultWriter {

    private static final int FLUSH_SIZE = 500;
    private final List<DetectionResult> buffer = new ArrayList<>();

    private final DetectionResultMapper mapper;
    private final StringRedisTemplate redisTemplate;

    public ResultWriter(DetectionResultMapper mapper, StringRedisTemplate redisTemplate) {
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
    }

    @LogExecutionTime
    public synchronized void write(DetectionResult result) {
        buffer.add(result);

        String redisKey = "task:" + result.getTaskId() + ":" + result.getStatus().toLowerCase();
        redisTemplate.opsForValue().increment(redisKey);

        if (buffer.size() >= FLUSH_SIZE) {
            flush();
        }
    }

    @Scheduled(fixedDelay = 5000)
    public synchronized void flushByTimeout() {
        if (!buffer.isEmpty()) {
            flush();
        }
    }

    private synchronized void flush() {
        if (buffer.isEmpty()) return;
        List<DetectionResult> snapshot = new ArrayList<>(buffer);
        buffer.clear();

        for (DetectionResult r : snapshot) {
            mapper.insert(r);
        }
        log.debug("批量写入 {} 条检测结果", snapshot.size());
    }
}

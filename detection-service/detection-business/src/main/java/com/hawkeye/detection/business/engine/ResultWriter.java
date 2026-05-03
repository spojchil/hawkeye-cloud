package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.mapper.DetectionResultMapper;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 检测结果批量写入器 — v3。
 * ConcurrentLinkedQueue 替代 synchronized + ArrayList。
 * saveBatch 替代循环单条 insert。
 */
@Slf4j
@Component
public class ResultWriter {

    private static final int FLUSH_SIZE = 500;
    private final Queue<DetectionResult> buffer = new ConcurrentLinkedQueue<>();

    private final DetectionResultMapper mapper;
    private final StringRedisTemplate redisTemplate;

    public ResultWriter(DetectionResultMapper mapper, StringRedisTemplate redisTemplate) {
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
    }

    public void write(DetectionResult result) {
        buffer.add(result);
        redisTemplate.opsForValue().increment(
                "task:" + result.getTaskId() + ":" + result.getStatus());
        if (buffer.size() >= FLUSH_SIZE) flush();
    }

    @Scheduled(fixedDelay = 5000)
    public void flushByTimeout() { flush(); }

    private void flush() {
        List<DetectionResult> batch = new ArrayList<>();
        for (int i = 0; i < FLUSH_SIZE; i++) {
            DetectionResult r = buffer.poll();
            if (r == null) break;
            batch.add(r);
        }
        if (!batch.isEmpty()) {
            mapper.insert(batch);
            log.debug("批量写入 {} 条检测结果", batch.size());
        }
    }
}

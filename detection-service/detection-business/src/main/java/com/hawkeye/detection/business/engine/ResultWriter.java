package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.mapper.DetectionResultMapper;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
 */
@Slf4j
@Component
public class ResultWriter {

    private static final int FLUSH_SIZE = 500;
    private final List<DetectionResult> buffer = new ArrayList<>();

    private final DetectionResultMapper mapper;
    private final RedisTemplate<String, String> redisTemplate;

    public ResultWriter(DetectionResultMapper mapper, RedisTemplate<String, String> redisTemplate) {
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 写入单条检测结果到本地缓冲区 + Redis 计数。
     */
    public synchronized void write(DetectionResult result) {
        buffer.add(result);

        // Redis 立即计数，不等批量 flush
        String statusKey = result.getStatus();
        String redisKey = "task:" + result.getTaskId() + ":" + statusKey.toLowerCase();
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

    private void flush() {
        if (buffer.isEmpty()) return;
        List<DetectionResult> snapshot = new ArrayList<>(buffer);
        buffer.clear();

        for (DetectionResult r : snapshot) {
            mapper.insert(r);
        }
        log.debug("批量写入 {} 条检测结果", snapshot.size());
    }
}

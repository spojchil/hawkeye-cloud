package com.hawkeye.task.business.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * task-service 业务配置。
 * 对应 application.yml 中的 {@code task:} 前缀。
 */
@Data
@Component
@ConfigurationProperties(prefix = "task")
public class TaskConfig {

    // TODO 轮询发现多余，之后删除
    /** 进度轮询间隔（毫秒），默认 2000ms */
    private long progressPollIntervalMs = 2000;

    /** 缓存配置 */
    private Cache cache = new Cache();

    @Data
    public static class Cache {
        /** L1 Caffeine 最大条目数 */
        private int maxSize = 2000;

        /** 逻辑过期 TTL（秒），过期后返回旧值 + 异步刷新 */
        private long logicalTtlSeconds = 1800;   // 30 min

        /** 物理过期 TTL（秒），Caffeine 逐出，应 > logicalTtl */
        private long physicalTtlSeconds = 5400;  // 90 min

        /** 防雪崩随机偏移上限（秒） */
        private long jitterSeconds = 300;         // 5 min

        /** 穿透保护：空值缓存 TTL（秒） */
        private long nullTtlSeconds = 60;         // 1 min
    }
}

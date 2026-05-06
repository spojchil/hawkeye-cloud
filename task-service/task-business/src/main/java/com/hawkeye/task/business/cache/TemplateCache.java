package com.hawkeye.task.business.cache;

import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hawkeye.task.business.config.TaskConfig;
import com.hawkeye.task.business.feign.VulServiceFeign;
import com.hawkeye.task.common.pojo.dto.TemplateDetectConfig;
import com.hawkeye.task.common.pojo.dto.VulTemplateBrief;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模板检测配置缓存。
 * L1 Caffeine + L2 Redis，Feign → VulTemplateBrief → 修剪 → TemplateDetectConfig。
 */
@Slf4j
@Component
public class TemplateCache {

    private static final String REDIS_PREFIX = "vul:template:";

    /** 缓存实体，带逻辑过期实体 */
    private record CacheEntry<T>(T data, long logicalExpireAt) {
        static <T> CacheEntry<T> of(T data, Duration logicalTtl) {
            return new CacheEntry<>(data, System.currentTimeMillis() + logicalTtl.toMillis());
        }

        boolean logicallyExpired() {
            return System.currentTimeMillis() > logicalExpireAt;
        }
    }

    /** 空值哨兵，用于判断是否为NULL，防护缓存穿透 */
    private static final TemplateDetectConfig NULL_SENTINEL = new TemplateDetectConfig();

    private final VulServiceFeign vulServiceFeign;
    private final StringRedisTemplate redisTemplate;

    /** L1 缓存 */
    private final Cache<Long, CacheEntry<TemplateDetectConfig>> caffeineCache;
    private final ExecutorService refreshExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /** 逻辑过期 TTL（秒），过期后返回旧值 + 异步刷新 */
    private final Duration logicalTtl;

    /** 穿透保护：空值缓存 TTL（秒） */
    private final Duration nullTtl;

    /** 防雪崩随机偏移上限（秒） */
    private final long jitterSeconds;

    public TemplateCache(VulServiceFeign vulServiceFeign, StringRedisTemplate redisTemplate,
                         TaskConfig config) {
        this.vulServiceFeign = vulServiceFeign;
        this.redisTemplate = redisTemplate;
        this.logicalTtl = Duration.ofSeconds(config.getCache().getLogicalTtlSeconds());
        this.nullTtl = Duration.ofSeconds(config.getCache().getNullTtlSeconds());
        this.jitterSeconds = config.getCache().getJitterSeconds();

        /* 构造缓存，带有最大项和随机物理过期时间限制的 */
        this.caffeineCache = Caffeine.newBuilder()
                .maximumSize(config.getCache().getMaxSize())
                .expireAfterWrite(randomTtl(Duration.ofSeconds(config.getCache().getPhysicalTtlSeconds())))
                .build();
    }

    public TemplateDetectConfig get(Long templateId) {
        CacheEntry<TemplateDetectConfig> entry = caffeineCache.getIfPresent(templateId);
        if (entry != null) {
            if (entry.data == NULL_SENTINEL) return null;
            if (!entry.logicallyExpired()) return entry.data;
            asyncRefresh(templateId);
            return entry.data;
        }

        String redisKey = REDIS_PREFIX + templateId;
        try {
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json != null) {
                TemplateDetectConfig cfg = JSON.parseObject(json, TemplateDetectConfig.class);
                if (cfg != null) {
                    caffeineCache.put(templateId, CacheEntry.of(cfg, logicalTtl));
                    return cfg;
                }
            }
        } catch (Exception e) {
            log.warn("Redis 不可用: {}", e.getMessage());
        }

        // Feign → 修剪 → 缓存
        log.debug("Feign 查 vul-service: templateId={}", templateId);
        try {
            VulTemplateBrief vo = vulServiceFeign.getTemplate(templateId).getData();
            if (vo != null) {
                TemplateDetectConfig cfg = TemplateDetectConfig.from(vo);
                caffeineCache.put(templateId, CacheEntry.of(cfg, logicalTtl));
                try {
                    redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(cfg), randomTtl(logicalTtl));
                } catch (Exception e) {
                    log.warn("Redis 写缓存失败: {}", e.getMessage());
                }
                return cfg;
            }
        } catch (Exception e) {
            log.error("Feign 查 vul-service 失败 templateId={}: {}", templateId, e.getMessage());
        }
        caffeineCache.put(templateId, CacheEntry.of(NULL_SENTINEL, nullTtl));
        return null;
    }

    public Map<Long, TemplateDetectConfig> batchGet(List<Long> templateIds) {
        Map<Long, TemplateDetectConfig> result = new LinkedHashMap<>();
        for (Long id : templateIds) {
            TemplateDetectConfig cfg = get(id);
            if (cfg != null) result.put(id, cfg);
        }
        return result;
    }

    private void asyncRefresh(Long templateId) {
        refreshExecutor.submit(() -> {
            try {
                VulTemplateBrief vo = vulServiceFeign.getTemplate(templateId).getData();
                if (vo != null) {
                    TemplateDetectConfig cfg = TemplateDetectConfig.from(vo);
                    caffeineCache.put(templateId, CacheEntry.of(cfg, logicalTtl));
                    try {
                        redisTemplate.opsForValue().set(
                                REDIS_PREFIX + templateId, JSON.toJSONString(cfg), randomTtl(logicalTtl));
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                log.warn("异步刷新缓存失败 templateId={}: {}", templateId, e.getMessage());
            }
        });
    }

    private Duration randomTtl(Duration base) {
        long ms = jitterSeconds * 1000;
        long jitter = ThreadLocalRandom.current().nextLong(-ms, ms);
        return Duration.ofMillis(Math.max(10_000, base.toMillis() + jitter));
    }
}

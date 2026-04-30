package com.hawkeye.task.business.cache;

import com.alibaba.fastjson2.JSON;
import com.hawkeye.task.business.config.TaskConfig;
import com.hawkeye.task.business.feign.VulServiceFeign;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
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
 * 模板检测配置缓存（task-service 侧）。
 * <p>
 * <b>缓存穿透：</b> Feign 返回 null 时缓存 NULL_SENTINEL（TTL 1min），
 * 防止攻击者用不存在的 ID 频繁穿透到 DB。
 * <p>
 * <b>缓存击穿：</b> 逻辑过期——缓存值包装 expireAt，过期后返回旧值并异步刷新，
 * 避免热点 key 过期瞬间大量请求穿透。
 * <p>
 * <b>缓存雪崩：</b> TTL 添加随机偏移（±jitter），避免大量 key 同时过期。
 */
@Slf4j
@Component
public class TemplateCache {

    private static final String REDIS_PREFIX = "vul:template:";

    /**
     * 逻辑过期包装。physicalExpireAt 之后 Caffeine 逐出，logicalExpireAt 之后返回旧值+异步刷新。
     */
    private record CacheEntry<T>(T data, long logicalExpireAt) {
        static <T> CacheEntry<T> of(T data, Duration logicalTtl) {
            return new CacheEntry<>(data, System.currentTimeMillis() + logicalTtl.toMillis());
        }
        boolean logicallyExpired() { return System.currentTimeMillis() > logicalExpireAt; }
    }

    /** 穿透标记 */
    private static final VulTemplateDetectDTO NULL_SENTINEL = new VulTemplateDetectDTO();

    private final VulServiceFeign vulServiceFeign;
    private final StringRedisTemplate redisTemplate;
    private final com.github.benmanes.caffeine.cache.Cache<Long, CacheEntry<VulTemplateDetectDTO>> caffeineCache;
    private final ExecutorService refreshExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final Duration logicalTtl;
    private final Duration physicalTtl;
    private final Duration nullTtl;
    private final long jitterSeconds;

    public TemplateCache(VulServiceFeign vulServiceFeign, StringRedisTemplate redisTemplate,
                         TaskConfig config) {
        this.vulServiceFeign = vulServiceFeign;
        this.redisTemplate = redisTemplate;
        this.logicalTtl = Duration.ofSeconds(config.getCache().getLogicalTtlSeconds());
        this.physicalTtl = Duration.ofSeconds(config.getCache().getPhysicalTtlSeconds());
        this.nullTtl = Duration.ofSeconds(config.getCache().getNullTtlSeconds());
        this.jitterSeconds = config.getCache().getJitterSeconds();
        this.caffeineCache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(config.getCache().getMaxSize())
                .expireAfterWrite(randomTtl(physicalTtl))
                .build();
    }

    public VulTemplateDetectDTO get(Long templateId) {
        CacheEntry<VulTemplateDetectDTO> entry = caffeineCache.getIfPresent(templateId);
        if (entry != null) {
            if (entry.data == NULL_SENTINEL) return null;          // 穿透保护命中
            if (!entry.logicallyExpired()) return entry.data;      // 正常命中
            // 逻辑过期：返回旧值，异步刷新
            asyncRefresh(templateId);
            return entry.data;
        }

        // L2 Redis
        String redisKey = REDIS_PREFIX + templateId;
        try {
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json != null) {
                VulTemplateDetectDTO dto = JSON.parseObject(json, VulTemplateDetectDTO.class);
                if (dto != null) {
                    caffeineCache.put(templateId, CacheEntry.of(dto, logicalTtl));
                    return dto;
                }
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过 L2 缓存: {}", e.getMessage());
        }

        // Feign 穿透
        log.info("缓存未命中，Feign 查 vul-service: templateId={}", templateId);
        VulTemplateDetectDTO dto = vulServiceFeign.getTemplate(templateId).getData();
        if (dto != null) {
            caffeineCache.put(templateId, CacheEntry.of(dto, logicalTtl));
            try {
                redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(dto), randomTtl(logicalTtl));
            } catch (Exception e) {
                log.warn("Redis 写缓存失败，不影响主流程: {}", e.getMessage());
            }
        } else {
            // 穿透保护：缓存空值，短 TTL
            caffeineCache.put(templateId, CacheEntry.of(NULL_SENTINEL, nullTtl));
        }
        return dto;
    }

    /**
     * 批量获取模板配置。
     * <p>
     * 不是真批量：循环调单个 {@link #get(Long)}，每次独立走缓存/Feign。
     * 模板数通常 ≤ 50，N 次调用可接受。
     */
    public Map<Long, VulTemplateDetectDTO> batchGet(List<Long> templateIds) {
        Map<Long, VulTemplateDetectDTO> result = new LinkedHashMap<>();
        for (Long id : templateIds) {
            VulTemplateDetectDTO dto = get(id);
            if (dto != null) result.put(id, dto);
        }
        return result;
    }

    private void asyncRefresh(Long templateId) {
        refreshExecutor.submit(() -> {
            try {
                log.debug("异步刷新缓存: templateId={}", templateId);
                VulTemplateDetectDTO dto = vulServiceFeign.getTemplate(templateId).getData();
                if (dto != null) {
                    caffeineCache.put(templateId, CacheEntry.of(dto, logicalTtl));
                    try {
                        redisTemplate.opsForValue().set(
                                REDIS_PREFIX + templateId,
                                JSON.toJSONString(dto),
                                randomTtl(logicalTtl));
                    } catch (Exception ignored) { }
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

package com.hawkeye.task.business.cache;

import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hawkeye.task.business.feign.VulServiceFeign;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模板检测配置缓存（task-service 侧）。
 * <p>
 * L1: Caffeine 本地缓存，L2: Redis 共享缓存，未命中 → Feign vul-service。
 * Redis 不可用时自动降级跳过 L2，直接走 Feign。
 * <p>
 * <b>缓存穿透：</b> 当前未处理。模板数量有限（~10,686）且 ID 可控，
 * 攻击者无法构造大量不存在 ID。后续可用布隆过滤器或缓存空值兜底。
 * <p>
 * <b>缓存击穿：</b> 待实现 Redis SET NX EX 分布式锁（见 TODO）。
 * <p>
 * <b>缓存雪崩：</b> TTL 添加随机偏移（±5min），避免大量 key 同时过期。
 */
@Slf4j
@Component
public class TemplateCache {

    private static final String REDIS_PREFIX = "vul:template:";
    /** 基础 TTL 30 分钟，实际使用时加随机偏移防雪崩 */
    private static final Duration TTL_BASE = Duration.ofMinutes(30);
    private static final Duration TTL_JITTER = Duration.ofMinutes(5);

    private final VulServiceFeign vulServiceFeign;
    private final StringRedisTemplate redisTemplate;
    private final Cache<Long, VulTemplateDetectDTO> caffeineCache;

    public TemplateCache(VulServiceFeign vulServiceFeign, StringRedisTemplate redisTemplate) {
        this.vulServiceFeign = vulServiceFeign;
        this.redisTemplate = redisTemplate;
        this.caffeineCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(randomTtl())
                .build();
    }

    public VulTemplateDetectDTO get(Long templateId) {
        VulTemplateDetectDTO cached = caffeineCache.getIfPresent(templateId);
        if (cached != null) return cached;

        String redisKey = REDIS_PREFIX + templateId;
        try {
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json != null) {
                VulTemplateDetectDTO dto = JSON.parseObject(json, VulTemplateDetectDTO.class);
                if (dto != null) {
                    caffeineCache.put(templateId, dto);
                    return dto;
                }
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过 L2 缓存: {}", e.getMessage());
        }

        // Feign 穿透
        log.info("缓存未命中，Feign 查 vul-service: templateId={}", templateId);
        // TODO 缓存击穿：此处应加 Redis SET NX EX 分布式锁，防止热点过期时大量并发穿透
        VulTemplateDetectDTO dto = vulServiceFeign.getTemplate(templateId).getData();
        if (dto != null) {
            try {
                redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(dto), randomTtl());
            } catch (Exception e) {
                log.warn("Redis 写缓存失败，不影响主流程: {}", e.getMessage());
            }
            caffeineCache.put(templateId, dto);
        }
        return dto;
    }

    /**
     * 批量获取模板配置。
     * <p>
     * <b>不是真批量：</b> 循环调用单个 {@link #get(Long)}，每次独立走缓存/Feign。
     * 设计原因：真批量需要一次收集未命中 ID 后调 Feign 批量接口，
     * 会与缓存击穿防护（分布式锁粒度变粗）耦合，大幅增加复杂度。
     * 模板数通常 ≤ 50，N 次 Feign 调用可接受。
     */
    public Map<Long, VulTemplateDetectDTO> batchGet(List<Long> templateIds) {
        Map<Long, VulTemplateDetectDTO> result = new LinkedHashMap<>();
        for (Long id : templateIds) {
            VulTemplateDetectDTO dto = get(id);
            if (dto != null) result.put(id, dto);
        }
        return result;
    }

    private static Duration randomTtl() {
        long seconds = TTL_BASE.toSeconds() + ThreadLocalRandom.current().nextLong(-TTL_JITTER.toSeconds(), TTL_JITTER.toSeconds());
        return Duration.ofSeconds(Math.max(10, seconds));
    }
}

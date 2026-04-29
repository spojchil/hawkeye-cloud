package com.hawkeye.detection.business.engine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hawkeye.detection.business.feign.VulServiceFeign;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 模板获取器——两级缓存。
 * <p>
 * L1: Caffeine 本地缓存（微秒级，Worker 进程内）
 * L2: Redis 共享缓存（毫秒级，跨 Worker 共享）
 * 未命中 → Feign vul-service
 */
@Slf4j
@Component
public class TemplateFetcher {

    private static final String REDIS_PREFIX = "vul:template:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final VulServiceFeign vulServiceFeign;
    private final RedisTemplate<String, String> redisTemplate;

    private final com.github.benmanes.caffeine.cache.Cache<Long, VulTemplateDTO> caffeineCache =
            Caffeine.newBuilder()
                    .maximumSize(2000)
                    .expireAfterWrite(TTL)
                    .build();

    public TemplateFetcher(VulServiceFeign vulServiceFeign, RedisTemplate<String, String> redisTemplate) {
        this.vulServiceFeign = vulServiceFeign;
        this.redisTemplate = redisTemplate;
    }

    public VulTemplateDTO fetch(Long vulId) {
        // L1: Caffeine
        VulTemplateDTO cached = caffeineCache.getIfPresent(vulId);
        if (cached != null) {
            return cached;
        }

        // L2: Redis
        String redisKey = REDIS_PREFIX + vulId;
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json != null) {
            VulTemplateDTO dto = com.alibaba.fastjson2.JSON.parseObject(json, VulTemplateDTO.class);
            caffeineCache.put(vulId, dto);
            return dto;
        }

        // L3: Feign
        log.debug("缓存未命中，Feign 查 vul-service: vulId={}", vulId);
        VulTemplateDTO dto = vulServiceFeign.getTemplate(vulId).getData();
        if (dto != null) {
            redisTemplate.opsForValue().set(redisKey, com.alibaba.fastjson2.JSON.toJSONString(dto), TTL);
            caffeineCache.put(vulId, dto);
        }
        return dto;
    }
}

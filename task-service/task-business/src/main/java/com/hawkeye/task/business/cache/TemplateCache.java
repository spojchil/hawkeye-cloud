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
import java.util.List;
import java.util.Map;

/**
 * 模板检测配置缓存（task-service 侧）。
 * L1: Caffeine 本地（2000条/30min），L2: Redis 共享（30min），未命中 → Feign vul-service。
 */
@Slf4j
@Component
public class TemplateCache {

    private static final String REDIS_PREFIX = "vul:template:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final VulServiceFeign vulServiceFeign;
    private final StringRedisTemplate redisTemplate;
    private final Cache<Long, VulTemplateDetectDTO> caffeineCache;

    public TemplateCache(VulServiceFeign vulServiceFeign, StringRedisTemplate redisTemplate) {
        this.vulServiceFeign = vulServiceFeign;
        this.redisTemplate = redisTemplate;
        this.caffeineCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(TTL)
                .build();
    }

    public VulTemplateDetectDTO get(Long templateId) {
        VulTemplateDetectDTO cached = caffeineCache.getIfPresent(templateId);
        if (cached != null) return cached;

        String redisKey = REDIS_PREFIX + templateId;
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json != null) {
            VulTemplateDetectDTO dto = JSON.parseObject(json, VulTemplateDetectDTO.class);
            caffeineCache.put(templateId, dto);
            return dto;
        }

        log.debug("缓存未命中，Feign 查 vul-service: templateId={}", templateId);
        VulTemplateDetectDTO dto = vulServiceFeign.getTemplate(templateId).getData();
        if (dto != null) {
            redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(dto), TTL);
            caffeineCache.put(templateId, dto);
        }
        return dto;
    }

    public Map<Long, VulTemplateDetectDTO> batchGet(List<Long> templateIds) {
        Map<Long, VulTemplateDetectDTO> result = new java.util.LinkedHashMap<>();
        for (Long id : templateIds) {
            VulTemplateDetectDTO dto = get(id);
            if (dto != null) result.put(id, dto);
        }
        return result;
    }
}

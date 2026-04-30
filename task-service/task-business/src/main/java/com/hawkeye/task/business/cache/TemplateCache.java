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

/**
 * 模板检测配置缓存（task-service 侧）。
 * L1: Caffeine 本地（2000条/30min），L2: Redis 共享（30min），未命中 → Feign vul-service。
 * Redis 不可用时自动降级，直接走 Feign。
 */
@Slf4j
@Component
public class TemplateCache {
    // TODO 让我看看你是如何解决缓存三兄弟的
    private static final String REDIS_PREFIX = "vul:template:";
    // TODO 不要硬编码，使用配置文件获取默认可以30
    private static final Duration TTL = Duration.ofMinutes(30);

    private final VulServiceFeign vulServiceFeign;
    private final StringRedisTemplate redisTemplate;
    private final Cache<Long, VulTemplateDetectDTO> caffeineCache;

    public TemplateCache(VulServiceFeign vulServiceFeign, StringRedisTemplate redisTemplate) {
        this.vulServiceFeign = vulServiceFeign;
        this.redisTemplate = redisTemplate;
        this.caffeineCache = Caffeine.newBuilder()
                // TODO同样不要使用硬编码
                .maximumSize(2000)
                // TODO 你的一二级缓存不仅时间相同而且没有随机值，完全不能防缓存雪崩
                .expireAfterWrite(TTL)
                .build();
    }

    public VulTemplateDetectDTO get(Long templateId) {
        VulTemplateDetectDTO cached = caffeineCache.getIfPresent(templateId);
        if (cached != null) return cached;

        // L2 Redis：不可用时降级跳过
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
            // TODO 这个应该是warn
            log.debug("Redis 不可用，跳过 L2 缓存: {}", e.getMessage());
        }

        // Feign 穿透
        // TODO 这个一个是info
        log.debug("缓存未命中，Feign 查 vul-service: templateId={}", templateId);
        VulTemplateDetectDTO dto = vulServiceFeign.getTemplate(templateId).getData();
        // TODO 你没有选择处理，缓存穿透的问题，一般处理方法是存储空值和布隆过滤器
        // TODO 你这个的结果是，攻击者请求大量空值，导致频频查库，服务宕机
        if (dto != null) {
            try {
                // TODO 你没有处理，缓存击穿，如果在数据刚刚过期时正在重建时有大量查询请求过来
                // TODO 都会查询数据库，解决方法一般是互斥锁，和逻辑过期，这里选择逻辑过期就可以
                redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(dto), TTL);
            } catch (Exception e) {
                // TODO 这个的信息等级不对吧不说是warn，你至少是个info吧，我感觉应该是warn
                log.debug("Redis 写缓存失败，不影响主流程: {}", e.getMessage());
            }
            caffeineCache.put(templateId, dto);
        }
        return dto;
    }

    public Map<Long, VulTemplateDetectDTO> batchGet(List<Long> templateIds) {
        Map<Long, VulTemplateDetectDTO> result = new LinkedHashMap<>();
        for (Long id : templateIds) {
            // TODO 这里是假批量，你远程调用还是单独发的，应该把需要远程调用的id收集起来，如何调用远程的批量查询方法
            // TODO 但是真批量会和缓存击穿，穿透作用导致复杂性大幅上升，我的建议是还是使用单查询，但是要说明
            VulTemplateDetectDTO dto = get(id);
            if (dto != null) result.put(id, dto);
        }
        return result;
    }
}

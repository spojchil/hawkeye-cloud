package com.hawkeye.detection.business.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.hawkeye.detection.business.engine.matcher.DslMatcher;
import com.hawkeye.detection.business.engine.matcher.MatcherStrategy;
import com.hawkeye.detection.business.engine.matcher.StatusMatcher;
import com.hawkeye.detection.business.engine.matcher.WordMatcher;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 匹配器责任链——按 and/or/negative 逻辑串联多个 matcher 进行判定。
 * <p>
 * 两层条件：
 * 1. 外层 matchers-condition: "and" / "or"（matcher 间的关系）
 * 2. 内层 condition: "and" / "or"（matcher 内多条规则的关系）
 */
@Slf4j
@Component
public class MatcherChain {

    private final List<MatcherStrategy> strategies = new ArrayList<>();

    public MatcherChain() {
        strategies.add(new WordMatcher());
        strategies.add(new StatusMatcher());
        strategies.add(new DslMatcher());
    }

    /**
     * 执行匹配判定。
     *
     * @param ctx     HTTP 响应上下文
     * @param matchersJson matchers JSON 字符串（含 matchers-condition 和 matchers 数组）
     * @return 是否命中
     */
    @SuppressWarnings("unchecked")
    public MatchResult match(HttpResponseContext ctx, String matchersJson) {
        if (matchersJson == null || matchersJson.isBlank()) {
            return MatchResult.NO_MATCH;
        }

        try {
            Map<String, Object> matchersObj = JSON.parseObject(matchersJson,
                    new TypeReference<Map<String, Object>>() {});
            if (matchersObj == null) return MatchResult.NO_MATCH;

            String outerCondition = (String) matchersObj.getOrDefault("matchers-condition", "or");
            Object matchersList = matchersObj.get("matchers");
            if (!(matchersList instanceof List<?> list)) return MatchResult.NO_MATCH;

            List<MatcherConfig> configs = new ArrayList<>();
            for (Object item : list) {
                // ★ 不要 JSON.toJSONString + parseObject（二次序列化），
                //    fastjson2 的 JSONObject 可直接转 Java Bean
                configs.add(JSON.toJavaObject(item, MatcherConfig.class));
            }

            if ("and".equals(outerCondition)) {
                return evaluateAnd(ctx, configs);
            } else {
                return evaluateOr(ctx, configs);
            }
        } catch (Exception e) {
            log.warn("matchers JSON 解析失败: {}", e.getMessage());
            return MatchResult.ERROR;
        }
    }

    private MatchResult evaluateAnd(HttpResponseContext ctx, List<MatcherConfig> configs) {
        for (MatcherConfig cfg : configs) {
            MatcherStrategy strategy = findStrategy(cfg.getType());
            if (strategy == null) continue;

            boolean result = evaluateInner(ctx, cfg, strategy);
            if (cfg.isNegative()) result = !result;
            if (!result) return MatchResult.NO_MATCH;
        }
        return MatchResult.MATCH;
    }

    private MatchResult evaluateOr(HttpResponseContext ctx, List<MatcherConfig> configs) {
        for (MatcherConfig cfg : configs) {
            MatcherStrategy strategy = findStrategy(cfg.getType());
            if (strategy == null) continue;

            boolean result = evaluateInner(ctx, cfg, strategy);
            if (cfg.isNegative()) result = !result;
            if (result) return MatchResult.MATCH;
        }
        return MatchResult.NO_MATCH;
    }

    private boolean evaluateInner(HttpResponseContext ctx, MatcherConfig cfg, MatcherStrategy strategy) {
        List<?> rules = getRules(cfg);
        if (rules == null || rules.isEmpty()) return false;

        if ("and".equals(cfg.getCondition())) {
            for (Object rule : rules) {
                if (!strategy.matchRule(ctx, cfg, rule)) return false;
            }
            return true;
        } else {
            // 默认 "or"
            for (Object rule : rules) {
                if (strategy.matchRule(ctx, cfg, rule)) return true;
            }
            return false;
        }
    }

    private List<?> getRules(MatcherConfig cfg) {
        return switch (cfg.getType()) {
            case "word" -> cfg.getWords();
            case "status" -> cfg.getStatus();
            case "dsl" -> cfg.getDsl();
            case "regex" -> cfg.getRegex();
            default -> null;
        };
    }

    private MatcherStrategy findStrategy(String type) {
        return strategies.stream()
                .filter(s -> s.getType().equals(type))
                .findFirst()
                .orElse(null);
    }

    public enum MatchResult {
        MATCH, NO_MATCH, ERROR
    }
}

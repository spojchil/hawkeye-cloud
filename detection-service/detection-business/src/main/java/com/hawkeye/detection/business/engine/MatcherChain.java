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
 * 匹配器责任链（v2 适配）。
 * <p>
 * v2: DetectionEngine 将步骤中的 matchers 转为 List&lt;MatcherConfig&gt; 后直接传入，
 * 不再在此处解析 JSON。
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
     * v2 接口：直接接受 matcher 列表 + 外层条件。
     */
    public MatchResult match(HttpResponseContext ctx, List<MatcherConfig> configs, String outerCondition) {
        if (configs == null || configs.isEmpty()) return MatchResult.NO_MATCH;

        if ("and".equals(outerCondition)) {
            return evaluateAnd(ctx, configs);
        } else {
            return evaluateOr(ctx, configs);
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

package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MatcherChain 匹配器责任链单元测试 (v2)")
class MatcherChainTest {

    private MatcherChain chain;
    private HttpResponseContext ctx;

    @BeforeEach
    void setUp() {
        chain = new MatcherChain();
        ctx = new HttpResponseContext();
        ctx.setStatusCode(200);
        ctx.setBody("Welcome to admin dashboard. User: root:x:0:0: login ok.");
        ctx.setContentType("text/html");
        ctx.setContentLength(100);
    }

    private static MatcherConfig cfg(String type, String part, String condition,
                                      Boolean negative, Boolean caseInsensitive,
                                      List<String> words, List<Integer> status,
                                      List<String> dsl, List<String> regex) {
        MatcherConfig c = new MatcherConfig();
        c.setType(type);
        c.setPart(part);
        c.setCondition(condition);
        c.setNegative(negative != null && negative);
        c.setCaseInsensitive(caseInsensitive != null && caseInsensitive);
        c.setWords(words);
        c.setStatus(status);
        c.setDsl(dsl);
        c.setRegex(regex);
        return c;
    }

    // ── Word Matcher ──

    @Test
    @DisplayName("word match: 单关键词命中")
    void wordMatchSingle() {
        List<MatcherConfig> list = List.of(cfg("word", "body", "or", null, null, List.of("admin"), null, null, null));
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, list, "or"));
    }

    @Test
    @DisplayName("word match: 任一关键词命中（condition=or）")
    void wordMatchOr() {
        List<MatcherConfig> list = List.of(cfg("word", "body", "or", null, null, List.of("missing", "admin", "nope"), null, null, null));
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, list, "or"));
    }

    @Test
    @DisplayName("word match: 全部关键词命中（condition=and）")
    void wordMatchAnd() {
        List<MatcherConfig> list = List.of(cfg("word", "body", "and", null, null, List.of("admin", "dashboard", "login"), null, null, null));
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, list, "or"));
    }

    @Test
    @DisplayName("word match: 部分关键词未命中（condition=and）→ NO_MATCH")
    void wordMatchAndFail() {
        List<MatcherConfig> list = List.of(cfg("word", "body", "and", null, null, List.of("admin", "not_found"), null, null, null));
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, list, "or"));
    }

    @Test
    @DisplayName("word match: caseInsensitive 忽略大小写")
    void wordMatchCaseInsensitive() {
        List<MatcherConfig> list = List.of(cfg("word", "body", "or", null, true, List.of("ADMIN"), null, null, null));
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, list, "or"));
    }

    @Test
    @DisplayName("word match: caseInsensitive=false 不忽略大小写 → NO_MATCH")
    void wordMatchCaseSensitive() {
        List<MatcherConfig> list = List.of(cfg("word", "body", "or", null, false, List.of("ADMIN"), null, null, null));
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, list, "or"));
    }

    @Test
    @DisplayName("word match: negative=true 反转结果 → 包含则 NO_MATCH")
    void wordMatchNegative() {
        List<MatcherConfig> list = List.of(cfg("word", "body", "or", true, null, List.of("admin"), null, null, null));
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, list, "or"));
    }

    // ── Status Matcher ──

    @Test
    @DisplayName("status match: 状态码在列表中")
    void statusMatch() {
        List<MatcherConfig> list = List.of(cfg("status", null, "or", null, null, null, List.of(200, 302), null, null));
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, list, "or"));
    }

    @Test
    @DisplayName("status match: 状态码不在列表中 → NO_MATCH")
    void statusNotMatch() {
        List<MatcherConfig> list = List.of(cfg("status", null, "or", null, null, null, List.of(404, 500), null, null));
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, list, "or"));
    }

    // ── DSL Matcher ──

    @Test
    @DisplayName("dsl match: contains + status_code 组合")
    void dslMatch() {
        List<MatcherConfig> list = List.of(cfg("dsl", null, "and", null, null, null, null, List.of("contains(body,'admin') && status_code == 200"), null));
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, list, "or"));
    }

    @Test
    @DisplayName("dsl match: 不满足 → NO_MATCH")
    void dslNotMatch() {
        List<MatcherConfig> list = List.of(cfg("dsl", null, "and", null, null, null, null, List.of("contains(body,'definitely_missing') && status_code == 500"), null));
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, list, "or"));
    }

    // ── 组合条件 ──

    @Test
    @DisplayName("matchers-condition=and: word + status 都满足")
    void outerAndBothMatch() {
        List<MatcherConfig> list = List.of(
                cfg("word", "body", "or", null, null, List.of("admin"), null, null, null),
                cfg("status", null, "or", null, null, null, List.of(200), null, null));
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, list, "and"));
    }

    @Test
    @DisplayName("matchers-condition=and: word 满足但 status 不满足 → NO_MATCH")
    void outerAndOneFail() {
        List<MatcherConfig> list = List.of(
                cfg("word", "body", "or", null, null, List.of("admin"), null, null, null),
                cfg("status", null, "or", null, null, null, List.of(404), null, null));
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, list, "and"));
    }

    @Test
    @DisplayName("matchers-condition=or: 任一满足 → MATCH")
    void outerOrOneMatch() {
        List<MatcherConfig> list = List.of(
                cfg("word", "body", "or", null, null, List.of("missing"), null, null, null),
                cfg("status", null, "or", null, null, null, List.of(200), null, null));
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, list, "or"));
    }

    // ── 边界情况 ──

    @Test
    @DisplayName("null matchers → NO_MATCH")
    void nullMatchers() {
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, null, "or"));
    }

    @Test
    @DisplayName("空 matchers → NO_MATCH")
    void emptyMatchers() {
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, Collections.emptyList(), "or"));
    }

    @Test
    @DisplayName("不支持的 matcher type → 跳过 → NO_MATCH")
    void unknownMatcherType() {
        List<MatcherConfig> list = List.of(cfg("unknown", "body", "or", null, null, List.of("x"), null, null, null));
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, list, "or"));
    }
}

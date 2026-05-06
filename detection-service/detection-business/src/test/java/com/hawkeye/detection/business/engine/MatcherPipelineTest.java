package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.matcher.*;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MatcherPipeline 匹配器编排测试 (v3)")
class MatcherPipelineTest {

    private MatcherPipeline pipeline;
    private HttpResponseContext ctx;

    @BeforeEach
    void setUp() {
        var registry = new MatcherRegistry(List.of(
                new WordMatcher(), new StatusMatcher(), new DslMatcher(), new RegexMatcher()));
        pipeline = new MatcherPipeline(registry);
        ctx = new HttpResponseContext();
        ctx.setStatusCode(200);
        ctx.setBody("Welcome to admin dashboard. User: root:x:0:0:");
        ctx.setContentType("text/html");
        ctx.setContentLength(100);
    }

    private static MatcherDef wordDef(String condition, List<String> words) {
        MatcherDef d = new MatcherDef(); d.setType("word"); d.setPart("body");
        d.setCondition(condition != null ? condition : "or"); d.setWords(words); return d;
    }
    private static MatcherDef statusDef(String condition, List<Integer> status) {
        MatcherDef d = new MatcherDef(); d.setType("status");
        d.setCondition(condition != null ? condition : "or"); d.setStatus(status); return d;
    }
    private static MatcherDef regexDef(String condition, List<String> regex) {
        MatcherDef d = new MatcherDef(); d.setType("regex"); d.setPart("body");
        d.setCondition(condition != null ? condition : "or"); d.setRegex(regex); return d;
    }

    // ── Word ──

    @Test @DisplayName("word: 单关键词命中")
    void wordMatchSingle() {
        assertTrue(pipeline.evaluate(ctx, List.of(wordDef("or", List.of("admin"))), "or"));
    }

    @Test @DisplayName("word: 任一命中 (or)")
    void wordMatchOr() {
        assertTrue(pipeline.evaluate(ctx, List.of(wordDef("or", List.of("missing", "admin", "nope"))), "or"));
    }

    @Test @DisplayName("word: 全部命中 (and)")
    void wordMatchAnd() {
        assertTrue(pipeline.evaluate(ctx, List.of(wordDef("and", List.of("Welcome", "admin"))), "or"));
    }

    @Test @DisplayName("word: and 部分失败")
    void wordMatchAndFail() {
        assertFalse(pipeline.evaluate(ctx, List.of(wordDef("and", List.of("Welcome", "nonexistent"))), "or"));
    }

    @Test @DisplayName("word: 大小写敏感")
    void wordCaseSensitive() {
        MatcherDef d = wordDef("or", List.of("WELCOME"));
        d.setCaseInsensitive(false);
        assertFalse(pipeline.evaluate(ctx, List.of(d), "or"));
    }

    @Test @DisplayName("word: 大小写不敏感")
    void wordCaseInsensitive() {
        MatcherDef d = wordDef("or", List.of("WELCOME"));
        d.setCaseInsensitive(true);
        assertTrue(pipeline.evaluate(ctx, List.of(d), "or"));
    }

    @Test @DisplayName("word: negative 取反")
    void wordNegative() {
        MatcherDef d = wordDef("or", List.of("admin"));
        d.setNegative(true);
        assertFalse(pipeline.evaluate(ctx, List.of(d), "or"));
    }

    // ── Status ──

    @Test @DisplayName("status: 命中")
    void statusMatch() {
        assertTrue(pipeline.evaluate(ctx, List.of(statusDef("or", List.of(200, 302))), "or"));
    }

    @Test @DisplayName("status: 未命中")
    void statusMiss() {
        assertFalse(pipeline.evaluate(ctx, List.of(statusDef("or", List.of(404))), "or"));
    }

    // ── Regex ──

    @Test @DisplayName("regex: 命中")
    void regexMatch() {
        assertTrue(pipeline.evaluate(ctx, List.of(regexDef("or", List.of("root:[x*]:\\d+:\\d+:"))), "or"));
    }

    @Test @DisplayName("regex: 未命中")
    void regexMiss() {
        assertFalse(pipeline.evaluate(ctx, List.of(regexDef("or", List.of("\\d{10,}"))), "or"));
    }

    // ── 组合 ──

    @Test @DisplayName("outer and: 全部命中")
    void outerAndAllMatch() {
        assertTrue(pipeline.evaluate(ctx, List.of(
                wordDef("or", List.of("admin")),
                statusDef("or", List.of(200))), "and"));
    }

    @Test @DisplayName("outer and: 一个失败")
    void outerAndOneFail() {
        assertFalse(pipeline.evaluate(ctx, List.of(
                wordDef("or", List.of("admin")),
                statusDef("or", List.of(404))), "and"));
    }

    @Test @DisplayName("outer or: 任一命中")
    void outerOrAny() {
        assertTrue(pipeline.evaluate(ctx, List.of(
                wordDef("or", List.of("missing")),
                statusDef("or", List.of(200))), "or"));
    }

    // ── 边界 ──

    @Test @DisplayName("空 matcher 列表 → false")
    void emptyMatchers() {
        assertFalse(pipeline.evaluate(ctx, List.of(), "or"));
    }

    @Test @DisplayName("null matcher 列表 → false")
    void nullMatchers() {
        assertFalse(pipeline.evaluate(ctx, null, "or"));
    }
}

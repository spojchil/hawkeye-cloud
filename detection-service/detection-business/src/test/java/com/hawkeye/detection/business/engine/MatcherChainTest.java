package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MatcherChain 匹配器责任链单元测试")
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

    // ── Word Matcher ──

    @Test
    @DisplayName("word match: 单关键词命中")
    void wordMatchSingle() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"word","part":"body","words":["admin"],"condition":"or"}]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("word match: 任一关键词命中（condition=or）")
    void wordMatchOr() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"word","part":"body","words":["missing","admin","nope"],"condition":"or"}]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("word match: 全部关键词命中（condition=and）")
    void wordMatchAnd() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"word","part":"body","words":["admin","dashboard","login"],"condition":"and"}]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("word match: 部分关键词未命中（condition=and）→ NO_MATCH")
    void wordMatchAndFail() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"word","part":"body","words":["admin","not_found"],"condition":"and"}]}
            """;
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("word match: caseInsensitive 忽略大小写")
    void wordMatchCaseInsensitive() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"word","part":"body","words":["ADMIN"],"condition":"or","caseInsensitive":true}]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("word match: caseInsensitive=false 不忽略大小写 → NO_MATCH")
    void wordMatchCaseSensitive() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"word","part":"body","words":["ADMIN"],"condition":"or","caseInsensitive":false}]}
            """;
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("word match: negative=true 反转结果 → 包含则 NO_MATCH")
    void wordMatchNegative() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"word","part":"body","words":["admin"],"condition":"or","negative":true}]}
            """;
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, matchersJson));
    }

    // ── Status Matcher ──

    @Test
    @DisplayName("status match: 状态码在列表中")
    void statusMatch() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"status","status":[200,302],"condition":"or"}]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("status match: 状态码不在列表中 → NO_MATCH")
    void statusNotMatch() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"status","status":[404,500],"condition":"or"}]}
            """;
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, matchersJson));
    }

    // ── DSL Matcher ──

    @Test
    @DisplayName("dsl match: contains + status_code 组合")
    void dslMatch() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"dsl","dsl":["contains(body,'admin') && status_code == 200"],"condition":"and"}]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("dsl match: contains 任一匹配")
    void dslContainsAny() {
        ctx.setBody("This is a test page");
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"dsl","dsl":["contains(body,'test')"],"condition":"and"}]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("dsl match: regex 正则匹配")
    void dslRegex() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"dsl","dsl":["regex('root:.*:0:0:', body)"],"condition":"and"}]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("dsl match: 不满足 → NO_MATCH")
    void dslNotMatch() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"dsl","dsl":["contains(body,'definitely_missing') && status_code == 500"],"condition":"and"}]}
            """;
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, matchersJson));
    }

    // ── 组合条件 ──

    @Test
    @DisplayName("matchers-condition=and: word + status 都满足")
    void outerAndBothMatch() {
        String matchersJson = """
            {"matchers-condition":"and","matchers":[
              {"type":"word","part":"body","words":["admin"],"condition":"or"},
              {"type":"status","status":[200],"condition":"or"}
            ]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("matchers-condition=and: word 满足但 status 不满足 → NO_MATCH")
    void outerAndOneFail() {
        String matchersJson = """
            {"matchers-condition":"and","matchers":[
              {"type":"word","part":"body","words":["admin"],"condition":"or"},
              {"type":"status","status":[404],"condition":"or"}
            ]}
            """;
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, matchersJson));
    }

    @Test
    @DisplayName("matchers-condition=or: 任一满足 → MATCH")
    void outerOrOneMatch() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[
              {"type":"word","part":"body","words":["missing"],"condition":"or"},
              {"type":"status","status":[200],"condition":"or"}
            ]}
            """;
        assertEquals(MatcherChain.MatchResult.MATCH, chain.match(ctx, matchersJson));
    }

    // ── 边界情况 ──

    @Test
    @DisplayName("null matchers → NO_MATCH")
    void nullMatchers() {
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, null));
    }

    @Test
    @DisplayName("空 matchers → NO_MATCH")
    void emptyMatchers() {
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, ""));
    }

    @Test
    @DisplayName("无效 JSON → ERROR")
    void invalidJson() {
        assertEquals(MatcherChain.MatchResult.ERROR, chain.match(ctx, "not json"));
    }

    @Test
    @DisplayName("不支持的 matcher type → 跳过")
    void unknownMatcherType() {
        String matchersJson = """
            {"matchers-condition":"or","matchers":[{"type":"unknown","words":["x"],"condition":"or"}]}
            """;
        // 无匹配，但不会抛异常
        assertEquals(MatcherChain.MatchResult.NO_MATCH, chain.match(ctx, matchersJson));
    }
}

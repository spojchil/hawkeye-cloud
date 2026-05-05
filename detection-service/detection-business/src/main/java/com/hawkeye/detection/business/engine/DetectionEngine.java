package com.hawkeye.detection.business.engine;

import com.common.utils.annotation.LogExecutionTime;
import com.hawkeye.detection.business.engine.model.HttpRequestConfig;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import com.hawkeye.detection.common.enums.DetectionStatusEnum;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage.HttpStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 检测引擎 — 核心编排器。
 * <p>
 * 职责：
 * 1. 接收 RocketMQ 消息，初始化检测上下文
 * 2. 解析执行流程（flow），逐步执行 HTTP 请求
 * 3. 协调变量解析、匹配判定、结果写入
 */
@Slf4j
@Component
public class DetectionEngine {

    private final HttpExecutor httpExecutor;
    private final MatcherPipeline matcherPipeline;
    private final ExtractorPipeline extractorPipeline;
    private final FlowInterpreter flowInterpreter;
    private final ResultWriter resultWriter;

    public DetectionEngine(HttpExecutor httpExecutor,
                           MatcherPipeline matcherPipeline,
                           ExtractorPipeline extractorPipeline,
                           FlowInterpreter flowInterpreter,
                           ResultWriter resultWriter) {
        this.httpExecutor = httpExecutor;
        this.matcherPipeline = matcherPipeline;
        this.extractorPipeline = extractorPipeline;
        this.flowInterpreter = flowInterpreter;
        this.resultWriter = resultWriter;
    }

    /**
     * 执行检测任务。
     *
     * @param msg RocketMQ 消息，包含完整的检测配置
     */
    @LogExecutionTime
    public void execute(TaskItemMessage msg) {
        long startTime = System.currentTimeMillis();

        try {
            // 校验消息有效性
            if (msg.getHttpSteps() == null || msg.getHttpSteps().isEmpty()) {
                writeResult(msg, DetectionStatusEnum.ERROR, null, null, "消息中无 HTTP 步骤", startTime);
                return;
            }

            // 初始化变量上下文
            VariableContext vars = new VariableContext(msg);

            // 解析 flow 表达式，逐步执行 HTTP 请求
            boolean matched = flowInterpreter.execute(msg.getHttpSteps(), msg.getFlow(),
                    step -> executeStep(step, vars));

            // 记录执行耗时
            Object dur = vars.get("duration");
            int durationMs = dur instanceof Long d ? d.intValue() : 0;

            // 写入结果
            DetectionStatusEnum status = matched ? DetectionStatusEnum.MATCHED : DetectionStatusEnum.NOT_MATCHED;
            writeResult(msg, status, null, matched ? LocalDateTime.now() : null, null, startTime);

        } catch (Exception e) {
            handleException(e, msg, startTime);
        }
    }

    /**
     * 写入检测结果。
     */
    private void writeResult(TaskItemMessage msg, DetectionStatusEnum status,
                             Integer responseStatusCode, LocalDateTime matchedAt,
                             String errorMessage, long startTime) {
        int durationMs = (int) (System.currentTimeMillis() - startTime);

        ResultWriter.DetectionResultUpdate result = new ResultWriter.DetectionResultUpdate(
                msg.getTaskId(),
                msg.getItemId(),
                status.getValue(),
                responseStatusCode,
                null,  // responseSize
                null,  // responseSummary
                null,  // matchedMatcher
                matchedAt,
                errorMessage,
                durationMs
        );

        resultWriter.write(result);
    }

    /**
     * 执行单个 HTTP 步骤。
     */
    private boolean executeStep(HttpStep step, VariableContext vars) {
        HttpRequestConfig config = buildRequestConfig(step);

        log.debug("executeStep: method={}, paths={}, raw={}",
                config.getMethod(), config.getPaths(),
                config.getRaw() != null ? config.getRaw().substring(0, Math.min(50, config.getRaw().length())) + "..." : "null");

        // 发送 HTTP 请求
        HttpResponseContext ctx = sendRequest(config, vars);

        // 更新变量上下文
        vars.updateFrom(ctx);

        // 执行提取器
        executeExtractors(step, ctx, vars);

        // 执行匹配器
        return executeMatchers(step, ctx);
    }

    private HttpRequestConfig buildRequestConfig(HttpStep step) {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setMethod(step.getMethod());
        config.setPaths(step.getPath());
        config.setHeaders(step.getHeaders());
        config.setBody(step.getBody());
        config.setRaw(step.getRaw());
        return config;
    }

    private HttpResponseContext sendRequest(HttpRequestConfig config, VariableContext vars) {
        try {
            if (config.getRaw() != null && !config.getRaw().isBlank()) {
                return httpExecutor.executeRaw(config.getRaw(), vars);
            } else {
                return httpExecutor.execute(config, vars);
            }
        } catch (IOException e) {
            // 改进错误消息：使用异常类名而不是可能为 null 的 getMessage
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            throw new RuntimeException("HTTP 请求失败: " + errorMsg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HTTP 请求被中断", e);
        }
    }

    private void executeExtractors(HttpStep step, HttpResponseContext ctx, VariableContext vars) {
        if (step.getExtractors() == null || step.getExtractors().isEmpty()) {
            return;
        }

        List<ExtractorDef> defs = step.getExtractors().stream()
                .map(this::toExtractorDef)
                .toList();
        extractorPipeline.extract(ctx, defs, vars);
    }

    private boolean executeMatchers(HttpStep step, HttpResponseContext ctx) {
        if (step.getMatchers() == null || step.getMatchers().isEmpty()) {
            return false;
        }

        List<MatcherDef> defs = step.getMatchers().stream()
                .map(this::toMatcherDef)
                .toList();

        String outerCondition = step.getMatchersCondition() != null ? step.getMatchersCondition() : "or";
        return matcherPipeline.evaluate(ctx, defs, outerCondition);
    }

    private ExtractorDef toExtractorDef(TaskItemMessage.Extractor e) {
        ExtractorDef ed = new ExtractorDef();
        ed.setType(e.getType());
        ed.setPart(e.getPart());
        ed.setName(e.getName());
        ed.setInternal(e.getInternal() != null && e.getInternal());
        ed.setGroupNum(e.getGroupNum());
        ed.setRegex(toStrList(e.getConfig(), "regex"));
        ed.setKval(toStrList(e.getConfig(), "kval"));
        return ed;
    }

    private MatcherDef toMatcherDef(TaskItemMessage.Matcher md) {
        MatcherDef def = new MatcherDef();
        def.setType(md.getType());
        def.setPart(md.getPart());
        def.setCondition(md.getCondition());
        def.setNegative(md.getNegative() != null && md.getNegative());
        def.setCaseInsensitive(md.getCaseInsensitive() != null && md.getCaseInsensitive());
        def.setWords(toStrList(md.getConfig(), "words"));
        def.setStatus(toIntList(md.getConfig(), "status"));
        def.setDsl(toStrList(md.getConfig(), "dsl"));
        def.setRegex(toStrList(md.getConfig(), "regex"));
        return def;
    }

    private void handleException(Exception e, TaskItemMessage msg, long startTime) {
        if (e instanceof RuntimeException && e.getCause() instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            log.warn("检测被中断 taskId={} itemId={}", msg.getTaskId(), msg.getItemId());
            writeResult(msg, DetectionStatusEnum.ERROR, null, null, "检测被中断", startTime);
        } else {
            log.error("检测异常 taskId={} itemId={}", msg.getTaskId(), msg.getItemId(), e);
            writeResult(msg, DetectionStatusEnum.ERROR, null, null,
                    e.getMessage() != null ? e.getMessage() : "内部错误", startTime);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStrList(Map<String, Object> config, String key) {
        if (config == null) return null;
        Object val = config.get(key);
        return val instanceof List<?> list ? list.stream().map(Object::toString).toList() : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> toIntList(Map<String, Object> config, String key) {
        if (config == null) return null;
        Object val = config.get(key);
        return val instanceof List<?> list ? list.stream().map(o ->
                o instanceof Number n ? n.intValue() : Integer.parseInt(o.toString())).toList() : null;
    }
}

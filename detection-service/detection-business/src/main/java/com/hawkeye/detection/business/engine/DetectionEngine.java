package com.hawkeye.detection.business.engine;

import com.common.utils.annotation.LogExecutionTime;
import com.hawkeye.detection.business.engine.model.HttpRequestConfig;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherConfig;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage.HttpStep;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检测引擎 — 纯 Worker，从消息体直接获取执行数据，不调任何外部 Feign。
 */
@Slf4j
@Component
public class DetectionEngine {

    private final ResultWriter resultWriter;
    private final HttpExecutor httpExecutor;
    private final MatcherChain matcherChain;
    private final ExtractorChain extractorChain;

    private static final Pattern FLOW_STEP = Pattern.compile("http\\((\\d+)\\)");

    public DetectionEngine(ResultWriter resultWriter,
                           HttpExecutor httpExecutor,
                           MatcherChain matcherChain,
                           ExtractorChain extractorChain) {
        this.resultWriter = resultWriter;
        this.httpExecutor = httpExecutor;
        this.matcherChain = matcherChain;
        this.extractorChain = extractorChain;
    }

    @LogExecutionTime
    public void execute(TaskItemMessage msg) {
        DetectionResult result = new DetectionResult();
        result.setTaskId(msg.getTaskId());
        result.setTaskItemId(msg.getItemId());
        result.setTemplateId(msg.getTemplateDbId());

        try {
            if (msg.getHttpSteps() == null || msg.getHttpSteps().isEmpty()) {
                writeError(result, "消息中无 HTTP 步骤");
                return;
            }

            VariableResolver resolver = new VariableResolver(
                    msg.getAssetProtocol(), msg.getAssetHost(),
                    msg.getAssetPort(), msg.getAssetPath(),
                    msg.getVariables());

            boolean matched = executeSteps(msg, resolver);

            result.setStatus(matched ? "matched" : "not_matched");
            Object dur = resolver.get("duration");
            result.setDurationMs(dur instanceof Long d ? d.intValue() : 0);
            if (matched) {
                result.setMatchedAt(LocalDateTime.now());
            }
            resultWriter.write(result);

        } catch (IOException e) {
            writeError(result, e.getMessage() != null ? e.getMessage() : "HTTP 请求失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeError(result, "HTTP 请求被中断");
        } catch (Exception e) {
            log.error("检测异常 taskId={} itemId={}: {}",
                    msg.getTaskId(), msg.getItemId(), e.getMessage(), e);
            try {
                writeError(result, e.getMessage() != null ? e.getMessage() : "未知错误");
            } catch (Exception ignored) { }
        }
    }

    private boolean executeSteps(TaskItemMessage msg, VariableResolver resolver)
            throws IOException, InterruptedException {
        String flow = msg.getFlow();
        List<HttpStep> steps = msg.getHttpSteps();

        if (flow == null || flow.isBlank()) {
            return executeStep(steps.get(0), resolver);
        }

        Matcher m = FLOW_STEP.matcher(flow);
        boolean allMatched = true;
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1)) - 1;
            if (idx >= steps.size()) continue;
            if (!executeStep(steps.get(idx), resolver)) {
                allMatched = false;
                break;
            }
        }
        return allMatched;
    }

    private boolean executeStep(HttpStep step, VariableResolver resolver)
            throws IOException, InterruptedException {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setMethod(step.getMethod());
        config.setPaths(step.getPath());
        config.setHeaders(step.getHeaders());
        config.setBody(step.getBody());
        config.setRaw(step.getRaw());

        HttpResponseContext ctx;
        if (config.getRaw() != null && !config.getRaw().isBlank()) {
            ctx = httpExecutor.executeRaw(config.getRaw(), resolver);
        } else {
            ctx = httpExecutor.execute(config, resolver);
        }

        resolver.put("body", ctx.getBody() != null ? ctx.getBody() : "");
        resolver.put("status_code", ctx.getStatusCode());
        resolver.put("content_type", ctx.getContentType());
        resolver.put("content_length", ctx.getContentLength());
        resolver.put("duration", ctx.getDurationMs());

        // 步骤级提取器
        if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
            extractorChain.extract(ctx, step.getExtractors(), resolver);
        }

        // 步骤级匹配器
        if (step.getMatchers() != null && !step.getMatchers().isEmpty()) {
            List<MatcherConfig> configs = step.getMatchers().stream().map(md -> {
                MatcherConfig mc = new MatcherConfig();
                mc.setType(md.getType());
                mc.setPart(md.getPart());
                mc.setCondition(md.getCondition());
                mc.setNegative(md.getNegative() != null && md.getNegative());
                mc.setCaseInsensitive(md.getCaseInsensitive() != null && md.getCaseInsensitive());
                mc.setWords(toStrList(md.getConfig(), "words"));
                mc.setStatus(toIntList(md.getConfig(), "status"));
                mc.setDsl(toStrList(md.getConfig(), "dsl"));
                mc.setRegex(toStrList(md.getConfig(), "regex"));
                return mc;
            }).toList();

            return matcherChain.match(ctx, configs,
                    step.getMatchersCondition() != null ? step.getMatchersCondition() : "or")
                    == MatcherChain.MatchResult.MATCH;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStrList(Map<String, Object> config, String key) {
        if (config == null) return null;
        Object val = config.get(key);
        return val instanceof List<?> list
                ? list.stream().map(Object::toString).toList() : null;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> toIntList(Map<String, Object> config, String key) {
        if (config == null) return null;
        Object val = config.get(key);
        return val instanceof List<?> list
                ? list.stream().map(o -> o instanceof Number n ? n.intValue()
                        : Integer.parseInt(o.toString())).toList() : null;
    }

    private void writeError(DetectionResult result, String msg) {
        result.setStatus("error");
        result.setErrorMessage(msg);
        resultWriter.write(result);
        log.warn("检测失败 taskId={} itemId={}: {}", result.getTaskId(), result.getTaskItemId(), msg);
    }
}

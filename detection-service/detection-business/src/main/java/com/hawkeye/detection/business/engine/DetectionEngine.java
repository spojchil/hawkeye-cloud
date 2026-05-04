package com.hawkeye.detection.business.engine;

import com.common.utils.annotation.LogExecutionTime;
import com.hawkeye.detection.business.engine.model.HttpRequestConfig;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import com.hawkeye.detection.common.enums.DetectionStatusEnum;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage.HttpStep;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @LogExecutionTime
    public void execute(TaskItemMessage msg) {
        DetectionResult result = new DetectionResult();
        result.setTaskId(msg.getTaskId());
        result.setTaskItemId(msg.getItemId());
        result.setTemplateId(msg.getTemplateDbId());
        result.setAssetId(msg.getAssetId());
        result.setTenantId(msg.getTenantId());

        try {
            if (msg.getHttpSteps() == null || msg.getHttpSteps().isEmpty()) {
                writeError(result, "消息中无 HTTP 步骤");
                return;
            }

            VariableContext vars = new VariableContext(msg);
            boolean matched = flowInterpreter.execute(msg.getHttpSteps(), msg.getFlow(),
                    step -> executeStep(step, vars));

            result.setStatus(matched ? DetectionStatusEnum.MATCHED.getValue() : DetectionStatusEnum.NOT_MATCHED.getValue());
            Object dur = vars.get("duration");
            result.setDurationMs(dur instanceof Long d ? d.intValue() : 0);
            if (matched) result.setMatchedAt(LocalDateTime.now());
            resultWriter.write(result);

        } catch (Exception e) {
            // 检查是否是被中断的情况
            if (e instanceof RuntimeException && e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                log.warn("检测被中断 taskId={} itemId={}", msg.getTaskId(), msg.getItemId());
                writeError(result, "检测被中断");
            } else {
                log.error("检测异常 taskId={} itemId={}", msg.getTaskId(), msg.getItemId(), e);
                writeError(result, e.getMessage() != null ? e.getMessage() : "内部错误");
            }
        }
    }

    private boolean executeStep(HttpStep step, VariableContext vars) {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setMethod(step.getMethod());
        config.setPaths(step.getPath());
        config.setHeaders(step.getHeaders());
        config.setBody(step.getBody());
        config.setRaw(step.getRaw());

        log.debug("executeStep: method={}, paths={}, raw={}", 
                config.getMethod(), config.getPaths(), 
                config.getRaw() != null ? config.getRaw().substring(0, Math.min(50, config.getRaw().length())) + "..." : "null");

        HttpResponseContext ctx;
        try {
            if (config.getRaw() != null && !config.getRaw().isBlank()) {
                ctx = httpExecutor.executeRaw(config.getRaw(), vars);
            } else {
                ctx = httpExecutor.execute(config, vars);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        vars.updateFrom(ctx);

        if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
            List<ExtractorDef> defs = step.getExtractors().stream().map(e -> {
                ExtractorDef ed = new ExtractorDef();
                ed.setType(e.getType()); ed.setPart(e.getPart()); ed.setName(e.getName());
                ed.setInternal(e.getInternal() != null && e.getInternal());
                ed.setGroupNum(e.getGroupNum());
                ed.setRegex(toStrList(e.getConfig(), "regex"));
                ed.setKval(toStrList(e.getConfig(), "kval"));
                return ed;
            }).toList();
            extractorPipeline.extract(ctx, defs, vars);
        }

        if (step.getMatchers() != null && !step.getMatchers().isEmpty()) {
            List<MatcherDef> defs = step.getMatchers().stream().map(md -> {
                MatcherDef def = new MatcherDef();
                def.setType(md.getType()); def.setPart(md.getPart());
                def.setCondition(md.getCondition());
                def.setNegative(md.getNegative() != null && md.getNegative());
                def.setCaseInsensitive(md.getCaseInsensitive() != null && md.getCaseInsensitive());
                def.setWords(toStrList(md.getConfig(), "words"));
                def.setStatus(toIntList(md.getConfig(), "status"));
                def.setDsl(toStrList(md.getConfig(), "dsl"));
                def.setRegex(toStrList(md.getConfig(), "regex"));
                return def;
            }).toList();
            return matcherPipeline.evaluate(ctx, defs,
                    step.getMatchersCondition() != null ? step.getMatchersCondition() : "or");
        }
        return false;
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

    private void writeError(DetectionResult result, String msg) {
        result.setStatus(DetectionStatusEnum.ERROR.getValue());
        result.setErrorMessage(msg);
        resultWriter.write(result);
    }
}

package com.hawkeye.detection.business.engine;

import com.common.utils.annotation.LogExecutionTime;
import com.hawkeye.detection.business.engine.model.HttpRequestConfig;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherConfig;
import com.hawkeye.detection.business.feign.AssetServiceFeign;
import com.hawkeye.detection.common.pojo.dto.AssetDTO;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO.HttpStepDetect;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO.MatcherDetect;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO.ExtractorDetect;
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
 * 检测引擎——编排一次 HTTP 探测 + 匹配判定的完整流程（v2）。
 * <p>
 * v2 适配：不再解析 httpRequests/matchers/extractors JSON 字符串，
 * 直接使用 VulTemplateDTO 的嵌套 httpSteps 结构。
 */
@Slf4j
@Component
public class DetectionEngine {

    private final TemplateFetcher templateFetcher;
    private final AssetServiceFeign assetServiceFeign;
    private final ResultWriter resultWriter;
    private final HttpExecutor httpExecutor;
    private final MatcherChain matcherChain;
    private final ExtractorChain extractorChain;

    private static final Pattern FLOW_STEP = Pattern.compile("http\\((\\d+)\\)");

    public DetectionEngine(TemplateFetcher templateFetcher,
                           AssetServiceFeign assetServiceFeign,
                           ResultWriter resultWriter,
                           HttpExecutor httpExecutor,
                           MatcherChain matcherChain,
                           ExtractorChain extractorChain) {
        this.templateFetcher = templateFetcher;
        this.assetServiceFeign = assetServiceFeign;
        this.resultWriter = resultWriter;
        this.httpExecutor = httpExecutor;
        this.matcherChain = matcherChain;
        this.extractorChain = extractorChain;
    }

    @LogExecutionTime
    public void execute(TaskItemMessage message) {
        DetectionResult result = new DetectionResult();
        result.setTaskId(message.getTaskId());
        result.setTaskItemId(message.getItemId());
        result.setAssetId(message.getAssetId());

        try {
            VulTemplateDTO template = templateFetcher.fetch(message.getVulId());
            if (template == null) {
                writeError(result, "模板不存在");
                return;
            }
            result.setTemplateId(message.getVulId());

            AssetDTO asset = assetServiceFeign.getAsset(message.getAssetId()).getData();
            if (asset == null) {
                writeError(result, "资产不存在");
                return;
            }

            VariableResolver resolver = new VariableResolver(asset, template);

            if (template.getHttpSteps() == null || template.getHttpSteps().isEmpty()) {
                writeError(result, "模板无 HTTP 步骤");
                return;
            }

            boolean matched = executeSteps(template, resolver);

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
                    message.getTaskId(), message.getItemId(), e.getMessage(), e);
            try {
                writeError(result, e.getMessage() != null ? e.getMessage() : "未知错误");
            } catch (Exception ignored) {
            }
        }
    }

    private boolean executeSteps(VulTemplateDTO template, VariableResolver resolver)
            throws IOException, InterruptedException {
        String flow = template.getFlow();
        List<HttpStepDetect> steps = template.getHttpSteps();

        if (flow == null || flow.isBlank()) {
            return executeSingleStep(steps, resolver);
        }

        // 多步模板：按 flow 表达式串行执行
        Matcher m = FLOW_STEP.matcher(flow);
        boolean allMatched = true;
        while (m.find()) {
            int stepIdx = Integer.parseInt(m.group(1)) - 1;
            if (stepIdx >= steps.size()) continue;
            boolean stepMatched = executeStep(steps.get(stepIdx), resolver);
            if (!stepMatched) {
                allMatched = false;
                break;
            }
        }
        return allMatched;
    }

    private boolean executeSingleStep(List<HttpStepDetect> steps, VariableResolver resolver)
            throws IOException, InterruptedException {
        return executeStep(steps.get(0), resolver);
    }

    private boolean executeStep(HttpStepDetect step, VariableResolver resolver)
            throws IOException, InterruptedException {
        // 构建 HTTP 请求配置
        HttpRequestConfig config = new HttpRequestConfig();
        config.setMethod(step.getMethod());
        config.setPaths(step.getPath());
        config.setHeaders(step.getHeaders());
        config.setBody(step.getBody());
        config.setRaw(step.getRaw());

        // HTTP 请求
        HttpResponseContext ctx;
        if (config.getRaw() != null && !config.getRaw().isBlank()) {
            ctx = httpExecutor.executeRaw(config.getRaw(), resolver);
        } else {
            ctx = httpExecutor.execute(config, resolver);
        }

        // 更新上下文变量
        resolver.put("body", ctx.getBody() != null ? ctx.getBody() : "");
        resolver.put("status_code", ctx.getStatusCode());
        resolver.put("content_type", ctx.getContentType());
        resolver.put("content_length", ctx.getContentLength());
        resolver.put("duration", ctx.getDurationMs());

        // 步骤级提取器
        if (step.getExtractors() != null) {
            extractorChain.extract(ctx, step.getExtractors(), resolver);
        }

        // 步骤级匹配器
        if (step.getMatchers() != null) {
            List<MatcherConfig> configs = step.getMatchers().stream().map(md -> {
                MatcherConfig mc = new MatcherConfig();
                mc.setType(md.getType());
                mc.setPart(md.getPart());
                mc.setCondition(md.getCondition());
                mc.setNegative(md.getNegative() != null && md.getNegative());
                mc.setCaseInsensitive(md.getCaseInsensitive() != null && md.getCaseInsensitive());
                mc.setWords(toList(md.getConfig(), "words"));
                mc.setStatus(toIntList(md.getConfig(), "status"));
                mc.setDsl(toList(md.getConfig(), "dsl"));
                mc.setRegex(toList(md.getConfig(), "regex"));
                return mc;
            }).toList();

            MatcherChain.MatchResult matchResult = matcherChain.match(ctx, configs,
                    step.getMatchersCondition() != null ? step.getMatchersCondition() : "or");
            return matchResult == MatcherChain.MatchResult.MATCH;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> toList(Map<String, Object> config, String key) {
        if (config == null) return null;
        Object val = config.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> toIntList(Map<String, Object> config, String key) {
        if (config == null) return null;
        Object val = config.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(o -> o instanceof Number n ? n.intValue() : Integer.parseInt(o.toString())).toList();
        }
        return null;
    }

    private void writeError(DetectionResult result, String errorMessage) {
        result.setStatus("error");
        result.setErrorMessage(errorMessage);
        resultWriter.write(result);
        log.warn("检测失败 taskId={} itemId={}: {}", result.getTaskId(), result.getTaskItemId(), errorMessage);
    }
}

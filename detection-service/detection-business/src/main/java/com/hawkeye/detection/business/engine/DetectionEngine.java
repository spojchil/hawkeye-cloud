package com.hawkeye.detection.business.engine;

import com.alibaba.fastjson2.JSON;
import com.hawkeye.detection.business.engine.model.HttpRequestConfig;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.feign.AssetServiceFeign;
import com.hawkeye.detection.common.pojo.dto.AssetDTO;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO;
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
 * 检测引擎——编排一次 HTTP 探测 + 匹配判定的完整流程。
 * <p>
 * 流程：
 * 1. 获取模板（缓存）    → TemplateFetcher
 * 2. 获取资产（Feign）    → AssetServiceFeign
 * 3. 构建变量上下文       → VariableResolver
 * 4. 解析执行计划         → 单步直接执行 / 多步按 flow 解析
 * 5. HTTP 请求            → HttpExecutor
 * 6. 提取变量             → ExtractorChain
 * 7. 匹配判定             → MatcherChain
 * 8. 写入结果             → ResultWriter
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
                           ResultWriter resultWriter) {
        this.templateFetcher = templateFetcher;
        this.assetServiceFeign = assetServiceFeign;
        this.resultWriter = resultWriter;
        this.httpExecutor = new HttpExecutor();
        this.matcherChain = new MatcherChain();
        this.extractorChain = new ExtractorChain();
    }

    /**
     * 执行单条 task_item 的检测。
     */
    public void execute(TaskItemMessage message) {
        DetectionResult result = new DetectionResult();
        result.setTaskId(message.getTaskId());
        result.setTaskItemId(message.getItemId());
        result.setAssetId(message.getAssetId());
        result.setVulId(message.getVulId());

        try {
            // 1. 获取模板
            VulTemplateDTO template = templateFetcher.fetch(message.getVulId());
            if (template == null) {
                writeError(result, "模板不存在");
                return;
            }

            // 2. 获取资产
            AssetDTO asset = assetServiceFeign.getAsset(message.getAssetId()).getData();
            if (asset == null) {
                writeError(result, "资产不存在");
                return;
            }

            // 3. 构建变量上下文
            VariableResolver resolver = new VariableResolver(asset, template);

            // 4-7. 执行检测：单步 or 多步
            boolean matched = executeSteps(template, resolver);

            // 8. 写入结果
            result.setStatus(matched ? "SUCCESS" : "NO_MATCH");
            Object dur = resolver.get("duration");
            result.setDurationMs(dur instanceof Long d ? d : 0L);
            resultWriter.write(result);

        } catch (IOException e) {
            writeError(result, (e.getMessage() != null ? e.getMessage() : "HTTP 请求失败"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeError(result, "HTTP 请求被中断");
        } catch (Exception e) {
            log.warn("检测异常 taskId={} itemId={}: {}", message.getTaskId(), message.getItemId(), e.getMessage());
            writeError(result, e.getMessage() != null ? e.getMessage() : "未知错误");
        }
    }

    private boolean executeSteps(VulTemplateDTO template, VariableResolver resolver)
            throws IOException, InterruptedException {
        String flow = template.getFlow();
        if (flow == null || flow.isBlank()) {
            // 单步模板
            return executeSingleStep(template, resolver);
        }

        // 多步模板：解析 flow 表达式
        Matcher m = FLOW_STEP.matcher(flow);
        boolean allMatched = true;
        while (m.find()) {
            int stepIdx = Integer.parseInt(m.group(1)) - 1;
            boolean stepMatched = executeStep(stepIdx, template, resolver);
            if (!stepMatched) {
                allMatched = false;
                break;
            }
        }
        return allMatched;
    }

    private boolean executeSingleStep(VulTemplateDTO template, VariableResolver resolver)
            throws IOException, InterruptedException {
        return executeStep(0, template, resolver);
    }

    @SuppressWarnings("unchecked")
    private boolean executeStep(int stepIdx, VulTemplateDTO template, VariableResolver resolver)
            throws IOException, InterruptedException {
        // 解析 http_requests JSON → HttpRequestConfig
        List<Map<String, Object>> requests = JSON.parseObject(template.getHttpRequests(), List.class);
        if (requests == null || stepIdx >= requests.size()) return false;

        Map<String, Object> stepMap = requests.get(stepIdx);
        HttpRequestConfig config = new HttpRequestConfig();
        config.setMethod((String) stepMap.get("method"));
        config.setPaths((List<String>) stepMap.get("path"));
        config.setHeaders((Map<String, String>) stepMap.get("headers"));
        config.setBody((String) stepMap.get("body"));
        config.setRaw((String) stepMap.get("raw"));

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

        // 提取变量（供后续步骤使用）
        extractorChain.extract(ctx, template.getExtractors(), resolver);

        // 匹配判定
        MatcherChain.MatchResult matchResult = matcherChain.match(ctx, template.getMatchers());
        return matchResult == MatcherChain.MatchResult.MATCH;
    }

    private void writeError(DetectionResult result, String errorMessage) {
        result.setStatus("ERROR");
        result.setErrorMessage(errorMessage);
        resultWriter.write(result);
        log.warn("检测失败 taskId={} itemId={}: {}", result.getTaskId(), result.getTaskItemId(), errorMessage);
    }
}

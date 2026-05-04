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

/**
 * 检测引擎 — 核心编排器。
 * <p>
 * 职责：
 * 1. 接收 RocketMQ 消息，初始化检测上下文
 * 2. 解析执行流程（flow），逐步执行 HTTP 请求
 * 3. 协调变量解析、匹配判定、结果写入
 * <p>
 * 执行流程：
 * <pre>
 *   TaskItemConsumer.onMessage(msg)
 *     └─ DetectionEngine.execute(msg)
 *           ├─ VariableContext.from(msg)     // 初始化变量上下文
 *           └─ FlowInterpreter.execute()    // 解析 flow 表达式
 *                 └─ executeStep(step)      // 执行单个 HTTP 步骤
 *                       ├─ HttpExecutor.execute()     // 发送请求
 *                       ├─ vars.updateFrom(ctx)       // 更新响应变量
 *                       ├─ ExtractorPipeline.extract() // 提取变量
 *                       └─ MatcherPipeline.evaluate()  // 匹配判定
 * </pre>
 * <p>
 * 设计要点：
 * - DetectionEngine 是纯编排器，不包含具体业务逻辑
 * - 每种能力（HTTP执行、匹配、提取）都是可插拔的组件
 * - 异常统一捕获，写入 error 结果
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
     * <p>
     * 主入口方法，由 TaskItemConsumer 调用。
     *
     * @param msg RocketMQ 消息，包含完整的检测配置
     */
    @LogExecutionTime
    public void execute(TaskItemMessage msg) {
        // 初始化检测结果
        DetectionResult result = initResult(msg);

        try {
            // 校验消息有效性
            if (msg.getHttpSteps() == null || msg.getHttpSteps().isEmpty()) {
                writeError(result, "消息中无 HTTP 步骤");
                return;
            }

            // 初始化变量上下文（包含资产信息、模板变量、随机函数等）
            VariableContext vars = new VariableContext(msg);

            // 解析 flow 表达式，逐步执行 HTTP 请求
            // flow 示例：null（顺序执行）、"http(1) && http(2)"（AND）、"http(1) || http(2)"（OR）
            boolean matched = flowInterpreter.execute(msg.getHttpSteps(), msg.getFlow(),
                    step -> executeStep(step, vars));

            // 设置检测结果状态
            result.setStatus(matched ? DetectionStatusEnum.MATCHED.getValue() : DetectionStatusEnum.NOT_MATCHED.getValue());

            // 记录执行耗时
            Object dur = vars.get("duration");
            result.setDurationMs(dur instanceof Long d ? d.intValue() : 0);

            // 记录匹配时间
            if (matched) {
                result.setMatchedAt(LocalDateTime.now());
            }

            // 写入结果（批量缓冲，定期刷盘）
            resultWriter.write(result);

        } catch (Exception e) {
            handleException(e, msg, result);
        }
    }

    /**
     * 初始化检测结果对象。
     */
    private DetectionResult initResult(TaskItemMessage msg) {
        DetectionResult result = new DetectionResult();
        result.setTaskId(msg.getTaskId());
        result.setTaskItemId(msg.getItemId());
        result.setTemplateId(msg.getTemplateDbId());
        result.setAssetId(msg.getAssetId());
        result.setTenantId(msg.getTenantId());
        return result;
    }

    /**
     * 执行单个 HTTP 步骤。
     * <p>
     * 流程：
     * 1. 构建 HTTP 请求配置
     * 2. 判断使用 raw 模式还是普通模式
     * 3. 发送请求，获取响应
     * 4. 更新变量上下文（body、status_code 等）
     * 5. 执行提取器（从响应中提取变量）
     * 6. 执行匹配器（判定是否命中漏洞）
     *
     * @param step HTTP 步骤定义
     * @param vars 变量上下文
     * @return true 如果匹配成功
     */
    private boolean executeStep(HttpStep step, VariableContext vars) {
        // 构建请求配置
        HttpRequestConfig config = buildRequestConfig(step);

        log.debug("executeStep: method={}, paths={}, raw={}",
                config.getMethod(), config.getPaths(),
                config.getRaw() != null ? config.getRaw().substring(0, Math.min(50, config.getRaw().length())) + "..." : "null");

        // 发送 HTTP 请求
        HttpResponseContext ctx = sendRequest(config, vars);

        // 更新变量上下文（body、status_code、content_type 等）
        vars.updateFrom(ctx);

        // 执行提取器（从响应中提取变量，写入 vars）
        executeExtractors(step, ctx, vars);

        // 执行匹配器（判定是否命中漏洞）
        return executeMatchers(step, ctx);
    }

    /**
     * 构建 HTTP 请求配置。
     */
    private HttpRequestConfig buildRequestConfig(HttpStep step) {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setMethod(step.getMethod());
        config.setPaths(step.getPath());
        config.setHeaders(step.getHeaders());
        config.setBody(step.getBody());
        config.setRaw(step.getRaw());
        return config;
    }

    /**
     * 发送 HTTP 请求。
     * <p>
     * 根据配置选择 raw 模式或普通模式：
     * - raw 模式：解析原始 HTTP 文本（如 "GET /path HTTP/1.1\nHost: xxx"）
     * - 普通模式：使用 method + path + headers + body 构建请求
     *
     * @param config 请求配置
     * @param vars   变量解析器
     * @return HTTP 响应上下文
     */
    private HttpResponseContext sendRequest(HttpRequestConfig config, VariableContext vars) {
        try {
            if (config.getRaw() != null && !config.getRaw().isBlank()) {
                // Raw 模式：解析原始 HTTP 文本
                return httpExecutor.executeRaw(config.getRaw(), vars);
            } else {
                // 普通模式：使用 method + path + headers + body
                return httpExecutor.execute(config, vars);
            }
        } catch (IOException e) {
            throw new RuntimeException("HTTP 请求失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HTTP 请求被中断", e);
        }
    }

    /**
     * 执行提取器。
     * <p>
     * 从 HTTP 响应中提取变量，写入 VariableContext。
     * 例如：从响应体中提取 token，供后续步骤使用。
     */
    private void executeExtractors(HttpStep step, HttpResponseContext ctx, VariableContext vars) {
        if (step.getExtractors() == null || step.getExtractors().isEmpty()) {
            return;
        }

        List<ExtractorDef> defs = step.getExtractors().stream()
                .map(this::toExtractorDef)
                .toList();
        extractorPipeline.extract(ctx, defs, vars);
    }

    /**
     * 执行匹配器。
     * <p>
     * 判定 HTTP 响应是否匹配漏洞特征。
     *
     * @return true 如果匹配成功
     */
    private boolean executeMatchers(HttpStep step, HttpResponseContext ctx) {
        if (step.getMatchers() == null || step.getMatchers().isEmpty()) {
            return false;
        }

        List<MatcherDef> defs = step.getMatchers().stream()
                .map(this::toMatcherDef)
                .toList();

        // 外层条件：and（所有 matcher 都匹配）或 or（任一 matcher 匹配）
        String outerCondition = step.getMatchersCondition() != null ? step.getMatchersCondition() : "or";
        return matcherPipeline.evaluate(ctx, defs, outerCondition);
    }

    /**
     * 将消息中的 Extractor 转换为 ExtractorDef。
     */
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

    /**
     * 将消息中的 Matcher 转换为 MatcherDef。
     */
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

    /**
     * 处理异常。
     * <p>
     * 区分中断异常和其他异常，写入错误结果。
     */
    private void handleException(Exception e, TaskItemMessage msg, DetectionResult result) {
        if (e instanceof RuntimeException && e.getCause() instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            log.warn("检测被中断 taskId={} itemId={}", msg.getTaskId(), msg.getItemId());
            writeError(result, "检测被中断");
        } else {
            log.error("检测异常 taskId={} itemId={}", msg.getTaskId(), msg.getItemId(), e);
            writeError(result, e.getMessage() != null ? e.getMessage() : "内部错误");
        }
    }

    /**
     * 从 config Map 中提取字符串列表。
     */
    @SuppressWarnings("unchecked")
    private static List<String> toStrList(Map<String, Object> config, String key) {
        if (config == null) return null;
        Object val = config.get(key);
        return val instanceof List<?> list ? list.stream().map(Object::toString).toList() : null;
    }

    /**
     * 从 config Map 中提取整数列表。
     */
    @SuppressWarnings("unchecked")
    private static List<Integer> toIntList(Map<String, Object> config, String key) {
        if (config == null) return null;
        Object val = config.get(key);
        return val instanceof List<?> list ? list.stream().map(o ->
                o instanceof Number n ? n.intValue() : Integer.parseInt(o.toString())).toList() : null;
    }

    /**
     * 写入错误结果。
     */
    private void writeError(DetectionResult result, String msg) {
        result.setStatus(DetectionStatusEnum.ERROR.getValue());
        result.setErrorMessage(msg);
        resultWriter.write(result);
    }
}

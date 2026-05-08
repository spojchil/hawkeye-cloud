package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.common.pojo.dto.TaskItemMessage.HttpStep;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 执行流编排——解析模板 flow 表达式（http(1)&&http(2)），按条件编排多步 HTTP 请求
 *
 * <p>flow 为空时执行单步，&& 全匹配，|| 任一匹配即止。</p>
 */
@Component
public class FlowInterpreter {

    private static final Pattern STEP_REF = Pattern.compile("http\\((\\d+)\\)");

    public boolean execute(List<HttpStep> steps, String flow, StepExecutor executor) {
        if (steps == null || steps.isEmpty()) return false;
        if (flow == null || flow.isBlank()) {
            return executor.execute(steps.get(0));
        }
        String expr = flow.trim();
        boolean isAndLogic = expr.contains("&&");
        boolean isOrLogic = expr.contains("||");
        if (isAndLogic) {
            return evaluateAnd(expr, steps, executor);
        } else if (isOrLogic) {
            return evaluateOr(expr, steps, executor);
        } else {
            return evaluateAnd(expr, steps, executor);
        }
    }

    private boolean evaluateAnd(String expr, List<HttpStep> steps, StepExecutor executor) {
        Matcher m = STEP_REF.matcher(expr);
        while (m.find()) {
            int stepIndex = Integer.parseInt(m.group(1)) - 1;
            if (stepIndex < 0 || stepIndex >= steps.size()) continue;
            if (!executor.execute(steps.get(stepIndex))) return false;
        }
        return true;
    }

    private boolean evaluateOr(String expr, List<HttpStep> steps, StepExecutor executor) {
        Matcher m = STEP_REF.matcher(expr);
        while (m.find()) {
            int stepIndex = Integer.parseInt(m.group(1)) - 1;
            if (stepIndex < 0 || stepIndex >= steps.size()) continue;
            if (executor.execute(steps.get(stepIndex))) return true;
        }
        return false;
    }

    @FunctionalInterface
    public interface StepExecutor {
        boolean execute(HttpStep step);
    }
}

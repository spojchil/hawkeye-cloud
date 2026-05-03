package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.common.pojo.dto.TaskItemMessage.HttpStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flow 表达式解释器。
 * null/空 → 顺序执行全部步骤。
 * "http(1) && http(2)" → AND 逻辑。
 * "http(1) || http(2)" → OR 逻辑。
 */
@Component
public class FlowInterpreter {

    private static final Pattern STEP_REF = Pattern.compile("http\\((\\d+)\\)");

    /**
     * @param steps    所有步骤
     * @param flow     flow 表达式（null=单步）
     * @param executor 单步执行器
     * @return true 如果整体匹配
     */
    public boolean execute(List<HttpStep> steps, String flow, StepExecutor executor) {
        if (steps == null || steps.isEmpty()) return false;

        // 单步：执行第一个
        if (flow == null || flow.isBlank()) {
            return executor.execute(steps.get(0));
        }

        // 多步：按 flow 表达式解析执行
        Matcher m = STEP_REF.matcher(flow);
        String expr = flow;
        // 先解析 && 逻辑
        if (expr.contains("&&")) {
            boolean allMatched = true;
            while (m.find()) {
                int idx = Integer.parseInt(m.group(1)) - 1;
                if (idx >= steps.size()) continue;
                if (!executor.execute(steps.get(idx))) {
                    allMatched = false;
                    break;
                }
            }
            return allMatched;
        }
        // || 逻辑：任一步骤匹配即成功
        if (expr.contains("||")) {
            while (m.find()) {
                int idx = Integer.parseInt(m.group(1)) - 1;
                if (idx >= steps.size()) continue;
                if (executor.execute(steps.get(idx))) return true;
            }
            return false;
        }
        // 默认顺序
        boolean allMatched = true;
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1)) - 1;
            if (idx >= steps.size()) continue;
            if (!executor.execute(steps.get(idx))) {
                allMatched = false;
                break;
            }
        }
        return allMatched;
    }

    @FunctionalInterface
    public interface StepExecutor {
        boolean execute(HttpStep step);
    }
}

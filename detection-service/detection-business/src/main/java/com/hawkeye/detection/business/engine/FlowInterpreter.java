package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.common.pojo.dto.TaskItemMessage.HttpStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flow 表达式解释器。
 * <p>
 * 负责解析模板中的 flow 字段，控制多个 HTTP 步骤的执行逻辑。
 * <p>
 * 支持的 flow 格式：
 * <ul>
 *   <li>null / 空 → 执行第一个步骤（单步模板）</li>
 *   <li>"http(1) && http(2)" → AND 逻辑，所有步骤都匹配才算成功</li>
 *   <li>"http(1) || http(2)" → OR 逻辑，任一步骤匹配即成功</li>
 *   <li>"http(1) && http(2) || http(3)" → 混合逻辑（从左到右解析）</li>
 * </ul>
 * <p>
 * 注意事项：
 * <ul>
 *   <li>步骤编号从 1 开始（与模板定义一致）</li>
 *   <li>如果步骤编号超出范围，会跳过该步骤</li>
 *   <li>短路求值：AND 遇到第一个 false 立即返回，OR 遇到第一个 true 立即返回</li>
 * </ul>
 * <p>
 * 示例：
 * <pre>
 *   // 单步模板
 *   flow: null
 *   // 等价于执行 http(1)
 *
 *   // 多步 AND
 *   flow: "http(1) && http(2)"
 *   // 先执行步骤1，如果匹配成功再执行步骤2，都成功才算漏洞存在
 *
 *   // 多步 OR
 *   flow: "http(1) || http(2)"
 *   // 先执行步骤1，如果匹配成功立即返回，否则执行步骤2
 * </pre>
 */
@Component
public class FlowInterpreter {

    /**
     * 匹配 flow 表达式中的步骤引用。
     * 例如：http(1) → group(1) = "1"
     */
    private static final Pattern STEP_REF = Pattern.compile("http\\((\\d+)\\)");

    /**
     * 执行 flow 表达式。
     *
     * @param steps    所有 HTTP 步骤列表
     * @param flow     flow 表达式（null 或空表示单步）
     * @param executor 单步执行器（回调函数）
     * @return true 如果整体匹配成功
     */
    public boolean execute(List<HttpStep> steps, String flow, StepExecutor executor) {
        if (steps == null || steps.isEmpty()) {
            return false;
        }

        // 单步模板：执行第一个步骤
        if (flow == null || flow.isBlank()) {
            return executor.execute(steps.get(0));
        }

        // 多步模板：解析 flow 表达式
        String expr = flow.trim();

        // 判断逻辑类型
        boolean isAndLogic = expr.contains("&&");
        boolean isOrLogic = expr.contains("||");

        if (isAndLogic) {
            return evaluateAnd(expr, steps, executor);
        } else if (isOrLogic) {
            return evaluateOr(expr, steps, executor);
        } else {
            // 默认：顺序执行（视为 AND 逻辑）
            return evaluateAnd(expr, steps, executor);
        }
    }

    /**
     * 评估 AND 逻辑。
     * <p>
     * 所有引用的步骤都必须匹配成功。
     * 短路求值：遇到第一个失败立即返回 false。
     */
    private boolean evaluateAnd(String expr, List<HttpStep> steps, StepExecutor executor) {
        Matcher m = STEP_REF.matcher(expr);
        while (m.find()) {
            int stepIndex = Integer.parseInt(m.group(1)) - 1; // 转换为 0-based 索引
            if (stepIndex < 0 || stepIndex >= steps.size()) {
                continue; // 跳过无效的步骤引用
            }
            if (!executor.execute(steps.get(stepIndex))) {
                return false; // 短路：遇到失败立即返回
            }
        }
        return true;
    }

    /**
     * 评估 OR 逻辑。
     * <p>
     * 任一步骤匹配成功即可。
     * 短路求值：遇到第一个成功立即返回 true。
     */
    private boolean evaluateOr(String expr, List<HttpStep> steps, StepExecutor executor) {
        Matcher m = STEP_REF.matcher(expr);
        while (m.find()) {
            int stepIndex = Integer.parseInt(m.group(1)) - 1; // 转换为 0-based 索引
            if (stepIndex < 0 || stepIndex >= steps.size()) {
                continue; // 跳过无效的步骤引用
            }
            if (executor.execute(steps.get(stepIndex))) {
                return true; // 短路：遇到成功立即返回
            }
        }
        return false;
    }

    /**
     * 单步执行器函数式接口。
     */
    @FunctionalInterface
    public interface StepExecutor {
        /**
         * 执行单个 HTTP 步骤。
         *
         * @param step HTTP 步骤定义
         * @return true 如果匹配成功
         */
        boolean execute(HttpStep step);
    }
}

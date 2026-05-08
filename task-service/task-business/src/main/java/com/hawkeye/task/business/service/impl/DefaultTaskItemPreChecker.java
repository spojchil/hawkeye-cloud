package com.hawkeye.task.business.service.impl;

import com.hawkeye.task.business.service.TaskItemPreChecker;
import com.hawkeye.task.common.pojo.dto.AssetBrief;
import com.hawkeye.task.common.pojo.dto.TemplateDetectConfig;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 默认预检器——过滤 OAST 机制模板（含 interactsh-url），后续可扩展
 */
@Slf4j
@Component
public class DefaultTaskItemPreChecker implements TaskItemPreChecker {

    /**
     * 匹配 interactsh-url 变量（OAST 机制）
     */
    private static final Pattern INTERACTSH_PATTERN = Pattern.compile(
            "\\{\\{interactsh-url}}|\\$\\{\\{interactsh-url}}",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean preCheck(AssetBrief asset, TemplateDetectConfig template) {
        /* 检查模板是否使用了 OAST 机制（interactsh-url） */
        if (usesOast(template)) {
            log.debug("跳过使用OAST机制的模板: {}", template.getYamlId());
            return false;
        }

        return true;
    }

    @Override
    public List<TaskItem> filterValidItems(List<TaskItem> items) {
        /* 当前实现：保留所有 items，在 preCheck 时逐个检查 */
        return items;
    }

    /**
     * 检查模板是否使用了 OAST 机制。
     * <p>
     * OAST（Out-of-band Application Security Testing）通过外部服务（如 interact.sh）
     * 验证漏洞是否存在。需要特殊的基础设施支持，当前系统暂不支持。
     * <p>
     * 常见的 OAST 变量：
     * - {{interactsh-url}}
     * - {{interactsh-domain}}
     * - {{interactsh-request}}
     */
    private boolean usesOast(TemplateDetectConfig template) {
        if (template.getHttpSteps() == null) {
            return false;
        }
        return template.getHttpSteps().stream()
                .anyMatch(this::stepUsesOast);
    }

    private boolean stepUsesOast(TemplateDetectConfig.HttpStepConfig step) {
        /* 检查 raw 请求中是否包含 interactsh-url */
        if (step.getRaw() != null && INTERACTSH_PATTERN.matcher(step.getRaw()).find()) {
            return true;
        }

        /* 检查 path 中是否包含 interactsh-url */
        if (step.getPath() != null) {
            for (String p : step.getPath()) {
                if (p != null && INTERACTSH_PATTERN.matcher(p).find()) {
                    return true;
                }
            }
        }

        /* 检查 matchers 的 config 中是否包含 interactsh */
        if (step.getMatchers() != null) {
            for (var matcher : step.getMatchers()) {
                if (matcher.getConfig() != null) {
                    for (var entry : matcher.getConfig().values()) {
                        if (entry instanceof String s && INTERACTSH_PATTERN.matcher(s).find()) {
                            return true;
                        }
                        if (entry instanceof List<?> list) {
                            for (Object item : list) {
                                if (item instanceof String s && INTERACTSH_PATTERN.matcher(s).find()) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}

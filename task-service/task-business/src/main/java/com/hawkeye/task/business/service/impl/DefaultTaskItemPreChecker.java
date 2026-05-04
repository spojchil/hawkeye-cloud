package com.hawkeye.task.business.service.impl;

import com.hawkeye.task.business.service.TaskItemPreChecker;
import com.hawkeye.task.common.pojo.dto.AssetBrief;
import com.hawkeye.task.common.pojo.dto.TemplateDetectConfig;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 默认检测项预检器实现。
 * <p>
 * 当前实现：
 * 1. 过滤掉使用 payloads 的模板（暂不支持）
 * 2. 其他预检逻辑待扩展
 */
@Slf4j
@Component
public class DefaultTaskItemPreChecker implements TaskItemPreChecker {

    @Override
    public boolean preCheck(AssetBrief asset, TemplateDetectConfig template) {
        // 检查模板是否使用了 payloads（暂不支持）
        if (hasPayloads(template)) {
            log.debug("跳过使用payloads的模板: {}", template.getYamlId());
            return false;
        }

        return true;
    }

    @Override
    public List<TaskItem> filterValidItems(List<TaskItem> items) {
        // 当前实现：保留所有 items，在 preCheck 时逐个检查
        return items;
    }

    /**
     * 检查模板是否使用了 payloads。
     * <p>
     * payloads 用于爆破攻击（如路径爆破、默认登录），需要为每个 payload 值生成独立请求。
     * 当前系统暂不支持此功能，需要过滤掉。
     */
    private boolean hasPayloads(TemplateDetectConfig template) {
        if (template.getHttpSteps() == null) {
            return false;
        }
        return template.getHttpSteps().stream()
                .anyMatch(this::hasPayloads);
    }

    private boolean hasPayloads(TemplateDetectConfig.HttpStepConfig step) {
        if (step.getPayloads() == null) {
            return false;
        }
        // 检查 payloads 是否非空
        Map<String, Object> payloads = step.getPayloads();
        if (payloads.isEmpty()) {
            return false;
        }
        // 检查是否有有效的 payload 值
        return payloads.values().stream()
                .anyMatch(v -> v != null && !(v instanceof List<?> list && list.isEmpty()));
    }
}

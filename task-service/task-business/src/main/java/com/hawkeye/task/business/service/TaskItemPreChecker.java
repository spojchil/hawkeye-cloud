package com.hawkeye.task.business.service;

import com.hawkeye.task.common.pojo.dto.AssetBrief;
import com.hawkeye.task.common.pojo.dto.TemplateDetectConfig;
import com.hawkeye.task.common.pojo.entity.TaskItem;

import java.util.List;

/**
 * 检测项预检器——判断（资产×模板）组合是否有效
 */
public interface TaskItemPreChecker {

    /**
     * 预检单个检测项。
     *
     * @param asset    资产信息
     * @param template 模板配置
     * @return true=有效，false=无效（将跳过该检测项）
     */
    boolean preCheck(AssetBrief asset, TemplateDetectConfig template);

    /**
     * 批量预检，剔除无效检测项。
     *
     * @param items 待检测项列表
     * @return 有效的检测项列表
     */
    List<TaskItem> filterValidItems(List<TaskItem> items);
}

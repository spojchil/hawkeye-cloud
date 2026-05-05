package com.hawkeye.detection.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.detection.common.pojo.entity.TaskItemResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检测项结果 Mapper（用于更新 task_item 表）。
 */
@Mapper
public interface TaskItemResultMapper extends BaseMapper<TaskItemResult> {
}

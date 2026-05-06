package com.hawkeye.task.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import org.apache.ibatis.annotations.Mapper;

/** 检测项表 Mapper */
@Mapper
public interface TaskItemMapper extends BaseMapper<TaskItem> {
}

package com.hawkeye.task.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskItemMapper extends BaseMapper<TaskItem> {
}

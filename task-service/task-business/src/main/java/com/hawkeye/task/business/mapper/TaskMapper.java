package com.hawkeye.task.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.task.common.pojo.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/** 任务表 Mapper */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}

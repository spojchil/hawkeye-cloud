package com.hawkeye.task.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
import org.apache.ibatis.annotations.Mapper;

/** 检测结果表 Mapper（task-service 查询检测结果用） */
@Mapper
public interface DetectionResultMapper extends BaseMapper<DetectionResult> {
}

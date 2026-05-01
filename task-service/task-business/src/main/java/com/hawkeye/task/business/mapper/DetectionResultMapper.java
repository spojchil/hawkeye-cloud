package com.hawkeye.task.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DetectionResultMapper extends BaseMapper<DetectionResult> {
}

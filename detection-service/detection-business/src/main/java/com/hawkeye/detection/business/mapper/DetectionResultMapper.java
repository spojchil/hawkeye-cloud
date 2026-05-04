package com.hawkeye.detection.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.detection.common.pojo.entity.DetectionResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检测结果 Mapper。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，提供基础 CRUD 操作。
 * <p>
 * 主要用途：
 * <ul>
 *   <li>ResultWriter 批量插入检测结果</li>
 * </ul>
 */
@Mapper
public interface DetectionResultMapper extends BaseMapper<DetectionResult> {
}

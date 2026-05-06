package com.hawkeye.task.business.mapstruct;

import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.common.pojo.vo.task.TaskItemVO;
import org.mapstruct.Mapper;

/**
 * 检测项对象映射器。
 */
@Mapper(componentModel = "spring")
public interface TaskItemMapstruct {

    /** Entity → Response VO */
    TaskItemVO.Response toResponseVO(TaskItem item);
}

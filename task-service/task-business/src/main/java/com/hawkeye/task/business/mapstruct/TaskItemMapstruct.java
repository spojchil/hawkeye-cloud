package com.hawkeye.task.business.mapstruct;

import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.common.pojo.vo.task.TaskItemVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskItemMapstruct {

    TaskItemVO.Response toResponseVO(TaskItem item);
}

package com.hawkeye.task.business.mapstruct;

import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapstruct {

    Task toEntity(TaskVO.Request request);

    TaskVO.Response toResponseVO(Task task);

    PageTaskVO.Response toPageTaskVO(Task task);
}

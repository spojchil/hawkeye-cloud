package com.hawkeye.task.business.mapstruct;

import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {Collectors.class})
public interface TaskMapstruct {

    @Mapping(target = "targetIds", expression = "java(request.getAssetIds().stream().map(String::valueOf).collect(Collectors.joining(\",\")))")
    @Mapping(target = "vulIds", expression = "java(request.getTemplateIds().stream().map(String::valueOf).collect(Collectors.joining(\",\")))")
    Task toEntity(TaskVO.Request request);

    TaskVO.Response toResponseVO(Task task);

    PageTaskVO.Response toPageTaskVO(Task task);
}

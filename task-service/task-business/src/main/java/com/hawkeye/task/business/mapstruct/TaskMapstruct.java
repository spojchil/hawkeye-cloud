package com.hawkeye.task.business.mapstruct;

import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务对象映射器。
 */
@Mapper(componentModel = "spring", imports = {Collectors.class})
public interface TaskMapstruct {

    /** Request → Entity（assetIds/templateIds 转为逗号分隔字符串） */
    @Mapping(target = "targetIds", expression = "java(request.getAssetIds().stream().map(String::valueOf).collect(Collectors.joining(\",\")))")
    @Mapping(target = "vulIds", expression = "java(request.getTemplateIds().stream().map(String::valueOf).collect(Collectors.joining(\",\")))")
    Task toEntity(TaskVO.Request request);

    /** Entity → Response VO */
    TaskVO.Response toResponseVO(Task task);

    /** Entity → 分页 Response VO */
    PageTaskVO.Response toPageTaskVO(Task task);
}

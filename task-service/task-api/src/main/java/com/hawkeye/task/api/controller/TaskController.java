package com.hawkeye.task.api.controller;

import com.common.utils.response.ApiResponse;
import com.common.utils.response.ListResult;
import com.hawkeye.task.business.service.TaskService;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
@Tag(name = "任务管理")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "提交扫描任务")
    public ApiResponse<TaskVO.Response> create(@Valid @RequestBody TaskVO.Request request) {
        return ApiResponse.success(taskService.create(request));
    }

    @GetMapping
    @Operation(summary = "分页查询任务列表")
    public ApiResponse<ListResult<PageTaskVO.Response>> pageQuery(@ParameterObject PageTaskVO.Request request) {
        return ApiResponse.success(taskService.pageQuery(request));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "查询任务详情")
    public ApiResponse<TaskVO.Response> getById(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.getById(taskId));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "取消任务（仅 PENDING 状态）")
    public ApiResponse<Void> cancel(@PathVariable Long taskId) {
        taskService.cancel(taskId);
        return ApiResponse.success();
    }
}

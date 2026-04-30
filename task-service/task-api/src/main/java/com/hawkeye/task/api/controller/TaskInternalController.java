package com.hawkeye.task.api.controller;

import com.common.utils.response.ApiResponse;
import com.hawkeye.task.business.service.TaskItemService;
import com.hawkeye.task.common.pojo.vo.task.TaskItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// TODO 我思考了一下，可以让检测服务，直接写数据库，这样明显效率高，没必要追求完美无状态，而且你这也不是批量写入啊，你这个是典型的1+n
/**
 * 内部 Feign 接口：供 detection-service 回写 task_item 结果。
 * detection-service 完成 HTTP 探测后，通过此接口更新 task_item 的状态和结果。
 * 该接口不暴露到 API 网关（网关路由不包含 /task/internal/）。
 */
@RestController
@RequestMapping("/task/internal")
@RequiredArgsConstructor
@Tag(name = "任务内部接口")
public class TaskInternalController {

    private final TaskItemService taskItemService;

    @PutMapping("/items/{itemId}")
    @Operation(summary = "回写检测项结果（detection-service 内部调用）")
    public ApiResponse<TaskItemVO.Response> updateItemResult(@PathVariable Long itemId,
                                                              @RequestBody TaskItemVO.Request request) {
        return ApiResponse.success(taskItemService.updateResult(itemId, request));
    }
}

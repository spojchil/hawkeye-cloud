package com.hawkeye.task.common.pojo.vo.task;

import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class TaskItemVO {

    @Data
    public static class Request {

        @NotNull(message = "状态不能为空")
        private TaskItemStatusEnum status;

        private String result;
    }

    @Data
    public static class Response {

        private Long itemId;

        private Long taskId;

        private Long assetId;

        private Long vulId;

        private TaskItemStatusEnum status;

        private String result;
    }
}

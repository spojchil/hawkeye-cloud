package com.hawkeye.task.common.pojo.vo.task;

import com.hawkeye.task.common.enums.TaskStatusEnum;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

public class PageTaskVO {

    @Data
    public static class Request {

        @Min(value = 1, message = "页码不能小于1")
        private Integer page;

        @Min(value = 1, message = "每页条数不能小于1")
        private Integer pageSize;

        /**
         * 模糊查询的任务名，可为空
         */
        private String taskName;

        /**
         * 模糊查询的状态
         */
        private TaskStatusEnum status;
    }

    @Data
    public static class Response {

        private Long taskId;

        private String taskName;

        private TaskStatusEnum status;

        private Integer totalItems;

        private Integer completedItems;

        private Integer failedItems;

        private Integer priority;

        private LocalDateTime startTime;

        private LocalDateTime endTime;
    }
}

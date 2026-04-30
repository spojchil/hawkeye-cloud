package com.hawkeye.task.common.pojo.vo.task;

import com.hawkeye.task.common.enums.TaskStatusEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public class PageTaskVO {

    @Data
    public static class Request {

        // TODO 我感觉我的这些错误信息不太标准
        @Min(value = 1,message = "页数从1开始")
        private Integer page;

        @Size(min=10, max=500,message = "页的尺寸在10~500之间")
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

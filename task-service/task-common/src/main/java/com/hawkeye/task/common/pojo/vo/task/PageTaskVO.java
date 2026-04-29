package com.hawkeye.task.common.pojo.vo.task;

import com.hawkeye.task.common.enums.TaskStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

public class PageTaskVO {

    @Data
    public static class Request {

        private Integer page;

        private Integer pageSize;

        private String taskName;

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

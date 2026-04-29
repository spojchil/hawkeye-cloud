package com.hawkeye.task.common.pojo.vo.task;

import com.hawkeye.task.common.enums.TaskStatusEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

public class TaskVO {

    @Data
    public static class Request {

        @NotBlank(message = "任务名称不能为空")
        private String taskName;

        @NotBlank(message = "目标资产ID列表不能为空")
        private String targetIds;

        @NotBlank(message = "漏洞模板ID列表不能为空")
        private String vulIds;

        private Integer priority;
    }

    @Data
    public static class Response {

        private Long taskId;

        private String taskName;

        private String targetIds;

        private String vulIds;

        private TaskStatusEnum status;

        private Integer totalItems;

        private Integer completedItems;

        private Integer failedItems;

        private Integer priority;

        private LocalDateTime startTime;

        private LocalDateTime endTime;

        private String resultSummary;
    }
}

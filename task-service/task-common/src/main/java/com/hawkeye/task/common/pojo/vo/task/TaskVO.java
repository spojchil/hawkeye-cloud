package com.hawkeye.task.common.pojo.vo.task;

import com.hawkeye.task.common.enums.TaskStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class TaskVO {

    @Data
    public static class Request {
        @NotBlank(message = "任务名称不能为空")
        private String taskName;

        @NotEmpty(message = "资产ID列表不能为空")
        private List<Long> assetIds;

        @NotEmpty(message = "模板ID列表不能为空")
        private List<Long> templateIds;

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
        /** 检测项列表（仅详情接口返回） */
        private List<TaskItemDetail> items;

        @Data
        public static class TaskItemDetail {
            private Long itemId;
            private Long assetId;
            private Long vulId;
            private String status;
            private Integer responseStatusCode;
            private Integer durationMs;
            private String matchedMatcher;
            private String errorMessage;
        }
    }
}

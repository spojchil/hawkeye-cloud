package com.hawkeye.task.business.mq;

import lombok.Data;

@Data
public class WorkerLoad {

    private String workerId;

    private Integer activeThreads;

    private Integer maxThreads;

    private Long lastReportTime;

    public int getRemainingCapacity() {
        if (maxThreads == null || activeThreads == null) {
            return 0;
        }
        return maxThreads - activeThreads;
    }
}

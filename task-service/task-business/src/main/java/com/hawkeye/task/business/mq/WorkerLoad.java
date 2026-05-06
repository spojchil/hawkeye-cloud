package com.hawkeye.task.business.mq;

import lombok.Data;

/**
 * Worker 负载信息。
 * <p>
 * detection-service 每 5s 上报到 Redis，task-service 分发时读取。
 */
@Data
public class WorkerLoad {

    /** Worker ID（如 worker-192.168.1.10） */
    private String workerId;

    /** 当前活跃线程数 */
    private Integer activeThreads;

    /** 最大线程数 */
    private Integer maxThreads;

    /** 上报时间戳 */
    private Long lastReportTime;

    /** 剩余容量 */
    public int getRemainingCapacity() {
        if (maxThreads == null || activeThreads == null) {
            return 0;
        }
        return maxThreads - activeThreads;
    }
}

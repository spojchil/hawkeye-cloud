package com.hawkeye.detection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 检测服务启动器。
 * <p>
 * detection-service 是纯 RocketMQ Consumer，不暴露 REST API（无 Controller 层）。
 * 可多实例部署实现水平扩展（每个实例竞争消费同一 ConsumerGroup 下的消息）。
 * <p>
 * {@code @EnableScheduling} 用于 5s 定时上报 Worker 负载到 Redis，
 * 以及定时 flush 本地缓存的检测结果到 DB。
 */
@SpringBootApplication
@EnableScheduling
public class DetectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(DetectionApplication.class, args);
    }
}

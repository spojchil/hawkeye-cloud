package com.hawkeye.detection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 检测服务——纯 Worker 执行引擎，不暴露 REST 接口，通过 RocketMQ 消费消息
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class DetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DetectionApplication.class, args);
        log.info("检测服务启动完成（端口 8006），等待 RocketMQ 消息...");
    }
}

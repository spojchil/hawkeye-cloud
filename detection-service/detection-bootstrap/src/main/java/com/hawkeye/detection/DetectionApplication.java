package com.hawkeye.detection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class DetectionApplication {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(DetectionApplication.class, args);
        log.info("检测服务启动完成（端口 8006），等待 RocketMQ 消息...");
        // 非 web 应用无内嵌 Tomcat，用 CountDownLatch 保持 JVM 存活
        new CountDownLatch(1).await();
    }
}

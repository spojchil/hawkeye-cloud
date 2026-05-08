package com.hawkeye.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 任务调度服务——端口 8005
 */
@SpringBootApplication
@EnableFeignClients  /* 启用 Feign（asset-service、vul-service） */
@EnableAsync         /* 启用异步（splitAndDispatch） */
@EnableScheduling    /* 启用定时任务（TaskProgressScheduler） */
public class TaskApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskApplication.class, args);
    }
}

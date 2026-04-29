package com.hawkeye.detection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "com.hawkeye.detection.business.feign")
public class DetectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(DetectionApplication.class, args);
    }
}

package com.hawkeye.detection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 检测服务启动类。
 * <p>
 * detection-service 是纯 Worker 执行引擎，不暴露 REST 接口。
 * 通过 RocketMQ 消费检测任务消息，执行 HTTP 探测 + 匹配 + 提取，批量写入结果。
 * <p>
 * 启动配置：
 * <ul>
 *   <li>端口：8006（健康检查 + Nacos 注册）</li>
 *   <li>@EnableScheduling：启用定时任务（ResultWriter.flushByTimeout）</li>
 * </ul>
 * <p>
 * 核心组件：
 * <ul>
 *   <li>TaskItemConsumer - RocketMQ 消费者</li>
 *   <li>DetectionEngine - 检测引擎</li>
 *   <li>HttpExecutor - HTTP 执行器</li>
 *   <li>MatcherPipeline - 匹配器编排</li>
 *   <li>ExtractorPipeline - 提取器编排</li>
 *   <li>ResultWriter - 结果批量写入</li>
 * </ul>
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

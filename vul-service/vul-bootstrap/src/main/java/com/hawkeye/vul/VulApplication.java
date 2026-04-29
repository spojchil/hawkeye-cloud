package com.hawkeye.vul;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.hawkeye.vul", "com.common.utils"})
@MapperScan({"com.hawkeye.vul.business.mapper", "com.common.utils.mapper"})
public class VulApplication {
    public static void main(String[] args) {
        SpringApplication.run(VulApplication.class, args);
    }
}

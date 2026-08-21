package com.opsflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OpsFlow 启动类
 */
@SpringBootApplication
@MapperScan("com.opsflow.module.**.mapper")
@EnableScheduling
public class OpsFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsFlowApplication.class, args);
    }
}
package com.ape.marathon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 马拉松赛事管理系统 - 启动类
 * @MapperScan：扫描 mapper 包，将 Mapper 接口注入 Spring 容器
 */
@SpringBootApplication
@MapperScan("com.ape.marathon.mapper")
public class MarathonApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarathonApplication.class, args);
        System.out.println("===== 马拉松赛事管理系统启动成功，端口 8080 =====");
    }
}
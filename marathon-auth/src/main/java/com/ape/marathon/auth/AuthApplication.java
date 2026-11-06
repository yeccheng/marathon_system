package com.ape.marathon.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务启动类
 * scanBasePackages 覆盖 common 中的 GlobalExceptionHandler / JwtAutoConfiguration 等组件
 */
@SpringBootApplication(scanBasePackages = "com.ape.marathon")
@EnableDiscoveryClient
@MapperScan("com.ape.marathon.auth.mapper")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("===== marathon-auth 启动成功，端口 8101 =====");
    }
}

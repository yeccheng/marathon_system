package com.ape.marathon.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关启动类
 * JwtUtil 由 marathon-common 的 spring.factories（JwtAutoConfiguration）自动装配；
 * 仅扫描 gateway 包，避免加载 common 中依赖 Redis/MVC 的组件
 */
@SpringBootApplication(scanBasePackages = "com.ape.marathon.gateway")
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("===== marathon-gateway 启动成功，端口 8081 =====");
    }
}

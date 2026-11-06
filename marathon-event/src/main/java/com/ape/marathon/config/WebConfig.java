package com.ape.marathon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置 + 上传目录静态资源映射
 * 访问示例：http://localhost:8080/uploads/1710000000000.jpg
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 本地上传目录，如 E:/框架/新建文件夹/marathon_system/uploads/ */
    @Value("${marathon.upload.path}")
    private String uploadPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 业务 API + Knife4j 文档相关路径均可跨域（与 Knife4j 同源调试不冲突）
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")          // 含 Authorization（JWT）
                .exposedHeaders("Content-Disposition") // 前端可读下载文件名
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 将 URL /uploads/** 映射到本地 uploads 目录
     * 注意：file: 协议路径末尾必须带 /，Windows 下也可用正斜杠
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadPath;
        if (location == null) {
            location = "uploads/";
        }
        // 统一为正斜杠，并保证以 / 结尾
        location = location.replace("\\", "/");
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }
}

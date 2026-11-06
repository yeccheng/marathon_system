package com.ape.marathon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.util.Collections;
import java.util.List;

/**
 * Knife4j（OpenAPI2 / Swagger2）配置类
 *
 * 访问地址：http://localhost:8080/doc.html
 *
 * 说明：
 * 1. 扫描 com.ape.marathon.controller 下所有接口
 * 2. 配置全局 Authorization（Bearer JWT），方便在文档页直接调试受保护接口
 * 3. 与 WebConfig 的 CORS 不冲突：文档页同源访问；调试接口走 /api/**
 * 4. 与 Shiro 兼容：文档静态资源与 swagger 元数据路径需在 ShiroConfig 中 anon 放行
 */
@Configuration
@EnableSwagger2WebMvc
public class Knife4jConfig {

    /** JWT 在 Header 中的名称，与前端约定一致：Authorization: Bearer xxx */
    private static final String AUTH_HEADER = "Authorization";

    /**
     * 默认分组：马拉松赛事管理全部接口
     */
    @Bean
    public Docket marathonApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .groupName("马拉松赛事管理")
                .select()
                // 只扫描业务 Controller 包
                .apis(RequestHandlerSelectors.basePackage("com.ape.marathon.controller"))
                .paths(PathSelectors.any())
                .build()
                // 文档页右上角可填 JWT，调试时自动带到请求头
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    /**
     * 文档基本信息（标题、描述、版本）
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("马拉松赛事管理系统 API")
                .description("赛事 / 选手 / 报名 / 成绩 / 统计 接口文档，支持在线调试。"
                        + "受保护接口请先调用 POST /api/login 获取 token，"
                        + "再在文档页右上角 Authorize 中填写：Bearer {token}")
                .contact(new Contact("marathon", "", ""))
                .version("1.0.0")
                .build();
    }

    /**
     * 声明安全方案：Header 中的 Authorization
     */
    private List<ApiKey> securitySchemes() {
        return Collections.singletonList(new ApiKey("BearerToken", AUTH_HEADER, "header"));
    }

    /**
     * 哪些路径需要携带上述安全方案（此处全部 /api/**）
     */
    private List<SecurityContext> securityContexts() {
        return Collections.singletonList(
                SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        .forPaths(PathSelectors.regex("/api/.*"))
                        .build()
        );
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope scope = new AuthorizationScope("global", "accessEverything");
        return Collections.singletonList(
                new SecurityReference("BearerToken", new AuthorizationScope[]{scope})
        );
    }
}

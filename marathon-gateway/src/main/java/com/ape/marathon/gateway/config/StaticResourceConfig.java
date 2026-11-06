package com.ape.marathon.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.BodyInserters;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * 网关静态资源：HTML / assets / routes（与 /api/** 网关路由并存）
 */
@Configuration
public class StaticResourceConfig {

    @Bean
    public RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions
                .route(GET("/"), request ->
                        ServerResponse.ok().contentType(MediaType.TEXT_HTML)
                                .body(BodyInserters.fromResource(new ClassPathResource("static/index.html"))))
                .andRoute(GET("/index.html"), request ->
                        ServerResponse.ok().contentType(MediaType.TEXT_HTML)
                                .body(BodyInserters.fromResource(new ClassPathResource("static/index.html"))))
                .andRoute(GET("/admin.html"), request ->
                        ServerResponse.ok().contentType(MediaType.TEXT_HTML)
                                .body(BodyInserters.fromResource(new ClassPathResource("static/admin.html"))))
                .and(RouterFunctions.resources("/assets/**", new ClassPathResource("static/assets/")))
                .and(RouterFunctions.resources("/routes/**", new ClassPathResource("static/routes/")));
    }
}

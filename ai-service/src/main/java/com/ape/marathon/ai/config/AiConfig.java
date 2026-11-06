package com.ape.marathon.ai.config;

import com.ape.marathon.ai.tool.EventQueryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Bean
    ChatClient chatClient(
            ChatClient.Builder builder,
            EventQueryTools eventQueryTools,
            @Value("classpath:prompts/system.txt") Resource systemPrompt) throws IOException {
        String system = StreamUtils.copyToString(systemPrompt.getInputStream(), StandardCharsets.UTF_8);
        return builder
                .defaultSystem(system)
                .defaultTools(eventQueryTools)
                .build();
    }
}

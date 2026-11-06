package com.ape.marathon.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyStartupCheck.class);

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String model;

    @Override
    public void run(ApplicationArguments args) {
        String key = apiKey == null ? "" : apiKey.trim();
        if (key.isEmpty()) {
            log.warn("spring.ai.openai.api-key 为空：请配置 application-local.yml 或 DEEPSEEK_API_KEY");
            return;
        }
        String masked = key.length() <= 8
                ? "****"
                : key.substring(0, 4) + "****" + key.substring(key.length() - 4);
        log.info("LLM ready: baseUrl={}, model={}, apiKey={}", baseUrl, model, masked);
    }
}

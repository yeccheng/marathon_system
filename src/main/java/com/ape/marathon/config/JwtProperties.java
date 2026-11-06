package com.ape.marathon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 * 从 application.yml 中读取 jwt.secret / jwt.expire
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥（HS256，长度建议 >= 32 字符） */
    private String secret = "marathon-jwt-secret-key-2026-must-be-long-enough";

    /** 过期时间（毫秒），默认 2 小时 */
    private long expire = 7200000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }
}

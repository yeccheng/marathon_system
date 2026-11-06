package com.ape.marathon.util;

import com.ape.marathon.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 职责：生成 token、解析 token、校验是否过期
 * token 中携带 username，过期时间由配置 jwt.expire 控制（默认 2 小时）
 */
@Component
public class JwtUtil {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 根据配置密钥构建 HS256 签名密钥
     */
    private SecretKey getKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT token
     *
     * @param username 登录账号（写入 subject）
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + jwtProperties.getExpire());
        return Jwts.builder()
                .setSubject(username)           // 用户名
                .setIssuedAt(now)               // 签发时间
                .setExpiration(expireAt)        // 过期时间
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token，返回 Claims；解析失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 已过期：仍可从异常中取出 Claims，便于上层判断“过期”
            return e.getClaims();
        } catch (Exception e) {
            // 签名错误、格式错误等
            return null;
        }
    }

    /**
     * 从 token 中取出用户名（subject）
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : claims.getSubject();
    }

    /**
     * 判断 token 是否已过期
     * true = 已过期或无法解析；false = 仍有效
     */
    public boolean isExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            return expiration == null || expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            // 无法解析视为无效/过期
            return true;
        }
    }

    /**
     * 校验 token：能解析、未过期、含用户名则视为有效
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        if (isExpired(token)) {
            return false;
        }
        String username = getUsername(token);
        return username != null && !username.trim().isEmpty();
    }
}

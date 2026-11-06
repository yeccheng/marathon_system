package com.ape.marathon.shiro;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * 自定义 Shiro AuthenticationToken
 * 把前端传来的 JWT 字符串包装成 Shiro 可识别的登录凭证
 */
public class JwtToken implements AuthenticationToken {

    private static final long serialVersionUID = 1L;

    /** JWT 字符串 */
    private final String token;

    public JwtToken(String token) {
        this.token = token;
    }

    /**
     * 身份（这里直接用 token 本身作为 principal）
     */
    @Override
    public Object getPrincipal() {
        return token;
    }

    /**
     * 凭证（同样是 token）
     */
    @Override
    public Object getCredentials() {
        return token;
    }
}

package com.ape.marathon.dto;

/**
 * 登录请求体
 * 对应前端 POST /api/login 的 JSON：{"username":"admin","password":"123456"}
 */
public class LoginRequest {

    /** 管理员账号 */
    private String username;

    /** 密码（当前为明文校验，后续可升级 BCrypt） */
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

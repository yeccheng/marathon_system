package com.ape.marathon.controller;

import com.ape.marathon.common.Result;
import com.ape.marathon.dto.LoginRequest;
import com.ape.marathon.entity.Admin;
import com.ape.marathon.mapper.AdminMapper;
import com.ape.marathon.util.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录接口
 * POST /api/login
 * 请求体：{"username":"admin","password":"123456"}
 * 成功：{"code":200,"msg":"操作成功","data":{"token":"xxx"}}
 * 失败：{"code":500,"msg":"中文提示","data":null}
 *
 * 说明：密码暂为明文比对（admin 表明文存储），后续可升级 BCrypt
 */
@Api(tags = "登录认证")
@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 管理员登录，校验通过后签发 JWT
     */
    @ApiOperation("管理员登录（返回 JWT token）")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (request == null
                || request.getUsername() == null || request.getUsername().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return Result.fail("用户名和密码不能为空");
        }

        String username = request.getUsername().trim();
        String password = request.getPassword();

        Admin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            return Result.fail("用户名或密码错误");
        }

        // 明文比对（后续升级 BCrypt 时改这里）
        if (admin.getPassword() == null || !admin.getPassword().equals(password)) {
            return Result.fail("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(admin.getUsername());
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("token", token);
        return Result.success(data);
    }
}

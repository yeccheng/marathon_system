package com.ape.marathon.shiro;

import com.ape.marathon.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 过滤器
 * 拦截需要认证的请求：
 * 1. 从 Header 读取 Authorization: Bearer xxx
 * 2. 无 token / 校验失败 → 直接返回 HTTP 401 + 统一 Result JSON
 * 3. 有 token → 交给 JwtRealm 做 Subject.login
 *
 * 同时放行 OPTIONS 预检，并补齐跨域响应头（Shiro 过滤器早于 Spring MVC CORS）
 */
public class JwtFilter extends BasicHttpAuthenticationFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 是否允许访问：已登录则放行；未登录则尝试执行登录（校验 JWT）
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // CORS 预检直接放行
        if (RequestMethod.OPTIONS.name().equalsIgnoreCase(httpRequest.getMethod())) {
            return true;
        }

        // 没有 Authorization 头 → 未登录
        String authorization = httpRequest.getHeader("Authorization");
        if (authorization == null || authorization.trim().isEmpty()) {
            return false;
        }

        try {
            // 执行登录（内部会调用 JwtRealm）
            executeLogin(request, response);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Header 取出 Bearer token，构造 JwtToken 交给 Shiro
     */
    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authorization = httpRequest.getHeader("Authorization");
        String token = resolveToken(authorization);
        if (token == null) {
            throw new AuthenticationException("Authorization 格式错误，应为 Bearer <token>");
        }
        getSubject(request, response).login(new JwtToken(token));
        return true;
    }

    /**
     * 解析 Authorization 头：支持 "Bearer xxx" 或直接传 token
     */
    private String resolveToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = value.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        // 兼容前端直接传 token（无 Bearer 前缀）
        return value.isEmpty() ? null : value;
    }

    /**
     * 访问被拒绝时：返回 401 JSON，不跳转登录页（前后端分离）
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        writeUnauthorized(response, "未登录或登录已过期，请重新登录");
        return false;
    }

    /**
     * 过滤器前置：补跨域头，OPTIONS 直接 200
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        fillCorsHeaders(httpRequest, httpResponse);

        if (RequestMethod.OPTIONS.name().equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpStatus.OK.value());
            return false;
        }
        return super.preHandle(request, response);
    }

    /**
     * 写出 401 统一响应体
     */
    private void writeUnauthorized(ServletResponse response, String msg) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpResponse.setContentType("application/json;charset=UTF-8");
        Result<?> body = Result.fail(401, msg);
        httpResponse.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }

    /**
     * 为过滤器层响应补齐 CORS（否则 401 时浏览器可能读不到 body）
     */
    private void fillCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers",
                "Authorization,Content-Type,X-Requested-With,Accept,Origin");
        response.setHeader("Access-Control-Max-Age", "3600");
    }
}

package com.ape.marathon.config;

import com.ape.marathon.shiro.JwtFilter;
import com.ape.marathon.shiro.JwtRealm;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro 配置类
 *
 * 过滤器链说明（顺序很重要，LinkedHashMap 按插入顺序匹配）：
 * 1. /api/login  → anon（匿名，无需 token，登录接口本身）
 * 2. /api/**     → jwt （自定义 JwtFilter，必须携带有效 Bearer token）
 * 3. /**         → anon（静态资源 index.html 等放行）
 *
 * 无状态：关闭 Shiro Session 存储，完全依赖 JWT
 */
@Configuration
public class ShiroConfig {

    /**
     * SecurityManager：挂载 JwtRealm，并关闭 Session（JWT 无状态）
     */
    @Bean
    public DefaultWebSecurityManager securityManager(JwtRealm jwtRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(jwtRealm);

        // 禁用 Session：前后端分离 + JWT，不在服务端保存会话
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        securityManager.setSubjectDAO(subjectDAO);

        return securityManager;
    }

    /**
     * Shiro 过滤器工厂：注册 jwt 过滤器并配置 URL 规则
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(DefaultWebSecurityManager securityManager) {
        ShiroFilterFactoryBean factoryBean = new ShiroFilterFactoryBean();
        factoryBean.setSecurityManager(securityManager);

        // 注册自定义过滤器，名称 "jwt" 供过滤器链引用
        Map<String, Filter> filters = new LinkedHashMap<String, Filter>();
        filters.put("jwt", new JwtFilter());
        factoryBean.setFilters(filters);

        // 过滤器链定义（必须用 LinkedHashMap 保证匹配顺序）
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<String, String>();
        // 登录接口匿名访问
        filterChainDefinitionMap.put("/api/login", "anon");
        // 选手端公开接口（无需 JWT）
        filterChainDefinitionMap.put("/api/public/**", "anon");
        // Knife4j / Swagger 文档相关路径匿名放行（否则 /doc.html 会被拦或资源加载失败）
        filterChainDefinitionMap.put("/doc.html", "anon");
        filterChainDefinitionMap.put("/swagger-resources/**", "anon");
        filterChainDefinitionMap.put("/v2/api-docs/**", "anon");
        filterChainDefinitionMap.put("/v3/api-docs/**", "anon");
        filterChainDefinitionMap.put("/webjars/**", "anon");
        filterChainDefinitionMap.put("/swagger-ui.html", "anon");
        filterChainDefinitionMap.put("/swagger-ui/**", "anon");
        filterChainDefinitionMap.put("/favicon.ico", "anon");
        // 上传封面图静态访问（浏览器 <img src> 不带 token，必须匿名）
        filterChainDefinitionMap.put("/uploads/**", "anon");
        // 其余 API 必须走 JWT 认证
        filterChainDefinitionMap.put("/api/**", "jwt");
        // 静态页面、其它路径放行
        filterChainDefinitionMap.put("/**", "anon");
        factoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

        return factoryBean;
    }
}

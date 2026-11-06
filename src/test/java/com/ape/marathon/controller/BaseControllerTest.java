package com.ape.marathon.controller;

import com.ape.marathon.service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 集成测试基类。
 * <p>
 * 说明：
 * <ul>
 *   <li>使用 {@link SpringBootTest} 启动真实 Spring 上下文（含 MyBatis / Shiro）</li>
 *   <li>使用 {@link AutoConfigureMockMvc} 注入 MockMvc，模拟 HTTP 调用</li>
 *   <li>使用 {@link Transactional}：每个测试方法结束后自动回滚，不污染业务库</li>
 *   <li>每个测试前先登录拿 JWT，后续请求统一带 Authorization 头</li>
 *   <li>用 {@link MockBean} 替换 RedisService：缓存命中返回 null，写/删为无操作，
 *       避免本机未启 Redis 时赛事列表/成绩排名等用例失败（不改业务代码）</li>
 * </ul>
 * 运行前提：本机 MySQL（库 marathon_system）已启动，且存在 admin/123456 及示例数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * 测试环境 Mock 掉 Redis，聚焦业务接口与 MySQL 校验逻辑。
     * 默认：get→null（未命中），deleteByPrefix→0，set/setWithSmartTtl 空操作。
     */
    @MockBean
    protected RedisService redisService;

    /** 当前测试类持有的 Bearer Token（不含 Bearer 前缀） */
    protected String token;

    /**
     * 每个用例执行前：Stub Redis + 登录获取有效 JWT。
     * 与业务接口一致走真实鉴权链路，避免绕过 Shiro 导致测试失真。
     */
    @BeforeEach
    void loginAndPrepareToken() throws Exception {
        // ---------- Redis Stub：缓存未命中，写删不抛异常 ----------
        when(redisService.get(anyString())).thenReturn(null);
        when(redisService.deleteByPrefix(anyString())).thenReturn(0L);
        doNothing().when(redisService).set(anyString(), any(), anyLong(), any());
        doNothing().when(redisService).setWithSmartTtl(anyString(), any(), anyBoolean());
        doNothing().when(redisService).delete(anyString());

        String body = "{\"username\":\"admin\",\"password\":\"123456\"}";
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        this.token = root.path("data").path("token").asText();
    }

    /**
     * 给 MockMvc 请求补上 Authorization: Bearer xxx。
     */
    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }

    /**
     * 生成近似合法且尽量不冲突的 18 位身份证号（仅满足格式校验，用于测试插入）。
     * 末 4 位取纳秒时间戳，降低与种子数据撞车概率。
     */
    protected String uniqueIdCard() {
        long suffix = Math.abs(System.nanoTime() % 10000L);
        return String.format("11010119900101%04d", suffix);
    }

    /**
     * 生成不冲突的测试手机号（11 位，1 开头）。
     */
    protected String uniquePhone() {
        long n = Math.abs(System.nanoTime() % 100000000L);
        return String.format("139%08d", n);
    }
}

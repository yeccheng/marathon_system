package com.ape.marathon.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 选手管理模块单元/集成测试（RunnerController）。
 * 重点覆盖：参数校验、身份证唯一、有报名不可删、无报名可删。
 */
@DisplayName("选手管理接口测试")
class RunnerControllerTest extends BaseControllerTest {

    /**
     * 构造合法新增选手 JSON。
     */
    private String buildValidRunnerJson(String name, String idCard, String phone) {
        return "{"
                + "\"name\":\"" + name + "\","
                + "\"gender\":1,"
                + "\"idCard\":\"" + idCard + "\","
                + "\"phone\":\"" + phone + "\","
                + "\"birthDate\":\"1995-05-05\","
                + "\"emergencyContact\":\"测试联系人 13800000000\""
                + "}";
    }

    @Test
    @DisplayName("正常新增选手应返回 code=200 与 id")
    void shouldAddRunnerSuccessfully() throws Exception {
        mockMvc.perform(withAuth(post("/api/runner/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidRunnerJson("测试选手甲", uniqueIdCard(), uniquePhone())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("姓名为空应参数校验失败")
    void shouldRejectAddWhenNameBlank() throws Exception {
        mockMvc.perform(withAuth(post("/api/runner/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidRunnerJson("", uniqueIdCard(), uniquePhone())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("选手姓名不能为空")));
    }

    @Test
    @DisplayName("身份证号格式错误应参数校验失败")
    void shouldRejectAddWhenIdCardFormatInvalid() throws Exception {
        mockMvc.perform(withAuth(post("/api/runner/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidRunnerJson("格式错误选手", "12345", uniquePhone())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("身份证号")));
    }

    @Test
    @DisplayName("手机号格式错误应参数校验失败")
    void shouldRejectAddWhenPhoneInvalid() throws Exception {
        mockMvc.perform(withAuth(post("/api/runner/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidRunnerJson("手机错选手", uniqueIdCard(), "12345")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("手机号")));
    }

    @Test
    @DisplayName("性别越界应参数校验失败")
    void shouldRejectAddWhenGenderInvalid() throws Exception {
        String body = "{"
                + "\"name\":\"性别错选手\","
                + "\"gender\":3,"
                + "\"idCard\":\"" + uniqueIdCard() + "\","
                + "\"phone\":\"" + uniquePhone() + "\","
                + "\"birthDate\":\"1990-01-01\","
                + "\"emergencyContact\":\"联系人\""
                + "}";
        mockMvc.perform(withAuth(post("/api/runner/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("性别")));
    }

    @Test
    @DisplayName("业务校验：身份证号重复应失败（uk_idcard）")
    void shouldRejectDuplicateIdCard() throws Exception {
        // 种子数据张三身份证：610104199503101234
        mockMvc.perform(withAuth(post("/api/runner/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidRunnerJson("重复身份证选手", "610104199503101234", uniquePhone())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("正常修改选手档案应成功")
    void shouldUpdateRunnerSuccessfully() throws Exception {
        String body = "{"
                + "\"id\":1,"
                + "\"name\":\"张三\","
                + "\"gender\":1,"
                + "\"idCard\":\"610104199503101234\","
                + "\"phone\":\"13800138000\","
                + "\"birthDate\":\"1995-03-10\","
                + "\"emergencyContact\":\"李四,13900139000\""
                + "}";
        mockMvc.perform(withAuth(put("/api/runner/update"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("分页第一页应返回不超过 pageSize 条")
    void shouldListFirstPage() throws Exception {
        mockMvc.perform(withAuth(get("/api/runner/list"))
                        .param("pageNum", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(7)))
                .andExpect(jsonPath("$.data.list", hasSize(lessThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("分页最后一页：total≈7、pageSize=5 时 pageNum=2 有数据")
    void shouldListLastPage() throws Exception {
        mockMvc.perform(withAuth(get("/api/runner/list"))
                        .param("pageNum", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("按姓名模糊查询")
    void shouldListByNameFuzzy() throws Exception {
        mockMvc.perform(withAuth(get("/api/runner/list"))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("name", "张"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("查询存在的选手详情应成功")
    void shouldGetRunnerDetailSuccessfully() throws Exception {
        mockMvc.perform(withAuth(get("/api/runner/detail/{id}", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").isNotEmpty());
    }

    @Test
    @DisplayName("查询不存在的选手应业务失败")
    void shouldFailWhenRunnerDetailNotFound() throws Exception {
        mockMvc.perform(withAuth(get("/api/runner/detail/{id}", 99999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("选手不存在")));
    }

    @Test
    @DisplayName("业务校验：有报名记录的选手不能删除（答辩亮点）")
    void shouldRejectDeleteRunnerWithRegistrations() throws Exception {
        // 种子数据选手 id=1（张三）已有报名
        mockMvc.perform(withAuth(delete("/api/runner/delete/{id}", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("已有报名记录，不能删除")));
    }

    @Test
    @DisplayName("正常删除：无报名记录的选手可以删除")
    void shouldDeleteRunnerWithoutRegistrations() throws Exception {
        MvcResult addResult = mockMvc.perform(withAuth(post("/api/runner/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidRunnerJson("待删除选手", uniqueIdCard(), uniquePhone())))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long newId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(withAuth(delete("/api/runner/delete/{id}", newId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("删除不存在的选手应业务失败")
    void shouldFailWhenDeleteRunnerNotFound() throws Exception {
        mockMvc.perform(withAuth(delete("/api/runner/delete/{id}", 99999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("选手不存在")));
    }
}

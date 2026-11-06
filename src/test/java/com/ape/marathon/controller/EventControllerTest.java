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
 * 赛事管理模块单元/集成测试（EventController）。
 * 重点覆盖：参数校验、详情不存在、有报名不可删、无报名可删等业务规则。
 */
@DisplayName("赛事管理接口测试")
class EventControllerTest extends BaseControllerTest {

    /**
     * 构造一份合法的新增赛事 JSON（报名中、名额充足）。
     */
    private String buildValidEventJson(String name, int quota, int status) {
        return "{"
                + "\"eventName\":\"" + name + "\","
                + "\"eventType\":1,"
                + "\"eventDate\":\"2027-06-01\","
                + "\"startLocation\":\"西安市未央区\","
                + "\"distanceKm\":42.195,"
                + "\"quota\":" + quota + ","
                + "\"entryFee\":200.00,"
                + "\"status\":" + status
                + "}";
    }

    @Test
    @DisplayName("正常新增赛事应返回 code=200 并带回 id")
    void shouldAddEventSuccessfully() throws Exception {
        mockMvc.perform(withAuth(post("/api/event/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidEventJson("单元测试马拉松", 1000, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("赛事名称为空时应参数校验失败（code=500）")
    void shouldRejectAddWhenEventNameBlank() throws Exception {
        String body = "{"
                + "\"eventName\":\"\","
                + "\"eventType\":1,"
                + "\"eventDate\":\"2027-06-01\","
                + "\"startLocation\":\"西安\","
                + "\"distanceKm\":42.195,"
                + "\"quota\":100,"
                + "\"entryFee\":100,"
                + "\"status\":0"
                + "}";
        mockMvc.perform(withAuth(post("/api/event/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("赛事名称不能为空")));
    }

    @Test
    @DisplayName("赛事类型越界时应参数校验失败")
    void shouldRejectAddWhenEventTypeInvalid() throws Exception {
        String body = "{"
                + "\"eventName\":\"非法类型赛事\","
                + "\"eventType\":9,"
                + "\"eventDate\":\"2027-06-01\","
                + "\"startLocation\":\"西安\","
                + "\"distanceKm\":10,"
                + "\"quota\":100,"
                + "\"entryFee\":50,"
                + "\"status\":0"
                + "}";
        mockMvc.perform(withAuth(post("/api/event/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("赛事类型")));
    }

    @Test
    @DisplayName("名额边界值 quota=0 应被拒绝")
    void shouldRejectAddWhenQuotaZero() throws Exception {
        mockMvc.perform(withAuth(post("/api/event/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidEventJson("名额为0赛事", 0, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("总名额至少为 1")));
    }

    @Test
    @DisplayName("正常修改赛事信息应成功")
    void shouldUpdateEventSuccessfully() throws Exception {
        String body = "{"
                + "\"id\":1,"
                + "\"eventName\":\"2026西安国际马拉松(测试改名)\","
                + "\"eventType\":1,"
                + "\"eventDate\":\"2026-11-15\","
                + "\"startLocation\":\"西安大明宫\","
                + "\"distanceKm\":42.195,"
                + "\"quota\":15000,"
                + "\"entryFee\":200,"
                + "\"status\":0"
                + "}";
        mockMvc.perform(withAuth(put("/api/event/update"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("分页第一页：pageNum=1 应返回不超过 pageSize 条")
    void shouldListFirstPage() throws Exception {
        mockMvc.perform(withAuth(get("/api/event/list"))
                        .param("pageNum", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.list", hasSize(lessThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("分页最后一页：在 total=3、pageSize=2 时 pageNum=2 应有数据")
    void shouldListLastPage() throws Exception {
        mockMvc.perform(withAuth(get("/api/event/list"))
                        .param("pageNum", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pages", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("查询存在的赛事详情应返回剩余名额")
    void shouldGetEventDetailSuccessfully() throws Exception {
        mockMvc.perform(withAuth(get("/api/event/detail/{id}", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.eventName").isNotEmpty())
                .andExpect(jsonPath("$.data.remainQuota").isNumber());
    }

    @Test
    @DisplayName("查询不存在的赛事详情应业务失败")
    void shouldFailWhenEventDetailNotFound() throws Exception {
        mockMvc.perform(withAuth(get("/api/event/detail/{id}", 99999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("赛事不存在")));
    }

    @Test
    @DisplayName("业务校验：有报名记录的赛事不能删除（答辩亮点）")
    void shouldRejectDeleteEventWithRegistrations() throws Exception {
        // 种子数据中赛事 id=1 已有多条报名记录
        mockMvc.perform(withAuth(delete("/api/event/delete/{id}", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("已有报名记录，不能删除")));
    }

    @Test
    @DisplayName("正常删除：无报名且报名中的赛事可以删除")
    void shouldDeleteEventWithoutRegistrations() throws Exception {
        // 1. 先新增一场干净赛事
        MvcResult addResult = mockMvc.perform(withAuth(post("/api/event/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidEventJson("待删除测试赛事", 100, 0)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long newId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. 删除应成功
        mockMvc.perform(withAuth(delete("/api/event/delete/{id}", newId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("业务校验：非报名中状态的赛事即使无报名也不能删除")
    void shouldRejectDeleteWhenEventAlreadyStarted() throws Exception {
        // 新增 status=1（报名截止）的无报名赛事
        MvcResult addResult = mockMvc.perform(withAuth(post("/api/event/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidEventJson("已截止待删赛事", 50, 1)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long newId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(withAuth(delete("/api/event/delete/{id}", newId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("已开始或已结束")));
    }

    @Test
    @DisplayName("删除不存在的赛事应返回业务失败")
    void shouldFailWhenDeleteEventNotFound() throws Exception {
        mockMvc.perform(withAuth(delete("/api/event/delete/{id}", 99999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("赛事不存在")));
    }

    @Test
    @DisplayName("多条件查询：按名称模糊 + 类型 + 状态")
    void shouldListByMultipleConditions() throws Exception {
        mockMvc.perform(withAuth(get("/api/event/list"))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("eventName", "西安")
                        .param("eventType", "1")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray());
    }
}

package com.ape.marathon.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 比赛成绩模块单元/集成测试（ResultController）。
 * 重点覆盖：正常录入、参数校验、同一报名不可重复录入、排名分页。
 */
@DisplayName("比赛成绩接口测试")
class ResultControllerTest extends BaseControllerTest {

    /**
     * 构造合法成绩录入 JSON。
     * 默认使用报名 id=8（赛事2 张三，种子数据中尚无成绩）。
     */
    private String buildInputJson(long registrationId, int chipTime, int finishStatus) {
        return "{"
                + "\"registrationId\":" + registrationId + ","
                + "\"chipTime\":" + chipTime + ","
                + "\"netTime\":" + (chipTime - 100) + ","
                + "\"finishStatus\":" + finishStatus + ","
                + "\"finishTime\":\"2026-10-25 10:30:00\""
                + "}";
    }

    @Test
    @DisplayName("正常录入成绩应返回 code=200 与成绩 id")
    void shouldInputResultSuccessfully() throws Exception {
        // 报名 id=8：赛事2，种子数据未录入成绩
        mockMvc.perform(withAuth(post("/api/result/input"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildInputJson(8L, 12000, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("业务校验：同一报名记录不能重复录入成绩（答辩亮点）")
    void shouldRejectDuplicateResultInput() throws Exception {
        // 报名 id=1 在种子数据中已有成绩
        mockMvc.perform(withAuth(post("/api/result/input"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildInputJson(1L, 13000, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("已录入成绩")));
    }

    @Test
    @DisplayName("报名记录不存在时录入应失败")
    void shouldRejectInputWhenRegistrationNotFound() throws Exception {
        mockMvc.perform(withAuth(post("/api/result/input"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildInputJson(99999L, 10000, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("报名记录不存在")));
    }

    @Test
    @DisplayName("报名记录ID为空应参数校验失败")
    void shouldRejectInputWhenRegistrationIdNull() throws Exception {
        String body = "{"
                + "\"chipTime\":12000,"
                + "\"netTime\":11800,"
                + "\"finishStatus\":1,"
                + "\"finishTime\":\"2026-10-25 10:30:00\""
                + "}";
        mockMvc.perform(withAuth(post("/api/result/input"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("报名记录ID不能为空")));
    }

    @Test
    @DisplayName("完赛状态越界应参数校验失败")
    void shouldRejectInputWhenFinishStatusInvalid() throws Exception {
        mockMvc.perform(withAuth(post("/api/result/input"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildInputJson(9L, 11000, 9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("完赛状态")));
    }

    @Test
    @DisplayName("枪声成绩≤0应参数校验失败")
    void shouldRejectInputWhenChipTimeNotPositive() throws Exception {
        mockMvc.perform(withAuth(post("/api/result/input"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildInputJson(9L, 0, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("枪声成绩必须大于 0")));
    }

    @Test
    @DisplayName("正常修改成绩应成功")
    void shouldUpdateResultSuccessfully() throws Exception {
        // update 接口同样走 @Valid，需带齐必填字段
        String body = "{"
                + "\"id\":1,"
                + "\"registrationId\":1,"
                + "\"chipTime\":12700,"
                + "\"netTime\":12400,"
                + "\"finishStatus\":1,"
                + "\"finishTime\":\"2026-11-15 10:29:10\""
                + "}";
        mockMvc.perform(withAuth(put("/api/result/update"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("修改成绩时完赛状态非法应失败")
    void shouldRejectUpdateWhenFinishStatusInvalid() throws Exception {
        String body = "{"
                + "\"id\":1,"
                + "\"registrationId\":1,"
                + "\"chipTime\":12700,"
                + "\"netTime\":12400,"
                + "\"finishStatus\":5,"
                + "\"finishTime\":\"2026-11-15 10:29:10\""
                + "}";
        mockMvc.perform(withAuth(put("/api/result/update"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("完赛状态")));
    }

    @Test
    @DisplayName("赛事总排名-第一页应按净成绩升序返回")
    void shouldGetEventRankingFirstPage() throws Exception {
        mockMvc.perform(withAuth(get("/api/result/eventRanking/{eventId}", 1))
                        .param("pageNum", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.list", hasSize(lessThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("赛事总排名-最后一页")
    void shouldGetEventRankingLastPage() throws Exception {
        mockMvc.perform(withAuth(get("/api/result/eventRanking/{eventId}", 1))
                        .param("pageNum", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("男女分组排名应返回 maleList 与 femaleList")
    void shouldGetGenderRanking() throws Exception {
        mockMvc.perform(withAuth(get("/api/result/genderRanking/{eventId}", 1))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.maleList").isArray())
                .andExpect(jsonPath("$.data.femaleList").isArray());
    }

    @Test
    @DisplayName("年龄分组排名应返回四个年龄段列表")
    void shouldGetAgeRanking() throws Exception {
        mockMvc.perform(withAuth(get("/api/result/ageRanking/{eventId}", 1))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.group_29below").isArray())
                .andExpect(jsonPath("$.data.group_30_39").isArray())
                .andExpect(jsonPath("$.data.group_40_49").isArray())
                .andExpect(jsonPath("$.data.group_50up").isArray());
    }

    @Test
    @DisplayName("成绩列表分页查询应成功")
    void shouldListResults() throws Exception {
        mockMvc.perform(withAuth(get("/api/result/list"))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("eventId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(0)));
    }
}

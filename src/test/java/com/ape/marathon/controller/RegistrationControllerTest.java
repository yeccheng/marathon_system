package com.ape.marathon.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

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
 * 赛事报名模块单元/集成测试（RegistrationController）。
 * <p>
 * 答辩重点：防重复报名、名额刚好满/剩1、重复取消、嵌套详情结构。
 */
@DisplayName("赛事报名接口测试")
class RegistrationControllerTest extends BaseControllerTest {

    /**
     * 辅助：新增一名未报名选手，返回 runnerId。
     */
    private long createFreshRunner(String name) throws Exception {
        String body = "{"
                + "\"name\":\"" + name + "\","
                + "\"gender\":1,"
                + "\"idCard\":\"" + uniqueIdCard() + "\","
                + "\"phone\":\"" + uniquePhone() + "\","
                + "\"birthDate\":\"1998-08-08\","
                + "\"emergencyContact\":\"紧急联系人\""
                + "}";
        MvcResult result = mockMvc.perform(withAuth(post("/api/runner/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    /**
     * 辅助：新增一场报名中赛事，返回 eventId。
     *
     * @param quota           总名额
     * @param registeredCount 初始已报名人数（用于模拟边界）
     */
    private long createEventWithQuota(int quota, int registeredCount) throws Exception {
        String body = "{"
                + "\"eventName\":\"报名边界测试赛-" + System.nanoTime() + "\","
                + "\"eventType\":3,"
                + "\"eventDate\":\"2027-08-01\","
                + "\"startLocation\":\"测试起点\","
                + "\"distanceKm\":5.0,"
                + "\"quota\":" + quota + ","
                + "\"registeredCount\":" + registeredCount + ","
                + "\"entryFee\":50,"
                + "\"status\":0"
                + "}";
        MvcResult result = mockMvc.perform(withAuth(post("/api/event/add"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("正常提交报名应返回 registrationId")
    void shouldSubmitRegistrationSuccessfully() throws Exception {
        long runnerId = createFreshRunner("正常报名选手");
        String body = "{\"eventId\":1,\"runnerId\":" + runnerId + "}";

        mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data.registrationId").isNumber());
    }

    @Test
    @DisplayName("业务校验：同一选手同一赛事重复报名应失败（答辩亮点）")
    void shouldRejectDuplicateRegistration() throws Exception {
        // 种子数据：选手1 已报名赛事1
        String body = "{\"eventId\":1,\"runnerId\":1}";
        mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("已报名此赛事")));
    }

    @Test
    @DisplayName("赛事不存在时报名应失败")
    void shouldRejectSubmitWhenEventNotFound() throws Exception {
        String body = "{\"eventId\":99999,\"runnerId\":1}";
        mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("赛事不存在")));
    }

    @Test
    @DisplayName("选手不存在时报名应失败")
    void shouldRejectSubmitWhenRunnerNotFound() throws Exception {
        String body = "{\"eventId\":1,\"runnerId\":99999}";
        mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("选手不存在")));
    }

    @Test
    @DisplayName("赛事非报名中状态不允许报名")
    void shouldRejectSubmitWhenEventNotOpen() throws Exception {
        // 赛事2 status=1（报名截止）
        long runnerId = createFreshRunner("截止赛选手");
        String body = "{\"eventId\":2,\"runnerId\":" + runnerId + "}";
        mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("不允许报名")));
    }

    @Test
    @DisplayName("边界值：名额刚好剩1时报名应成功")
    void shouldSubmitWhenExactlyOneQuotaLeft() throws Exception {
        // quota=2，registeredCount=1 → 剩1个名额
        long eventId = createEventWithQuota(2, 1);
        long runnerId = createFreshRunner("剩一名额选手");
        String body = "{\"eventId\":" + eventId + ",\"runnerId\":" + runnerId + "}";

        mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registrationId").isNumber());
    }

    @Test
    @DisplayName("边界值：名额刚好满后再报名应失败（答辩亮点）")
    void shouldRejectSubmitWhenQuotaJustFull() throws Exception {
        // 1. 创建 quota=1 的空赛事
        long eventId = createEventWithQuota(1, 0);
        long runnerA = createFreshRunner("满额选手A");
        long runnerB = createFreshRunner("满额选手B");

        // 2. 第一人报名成功 → 名额刚好满
        mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":" + eventId + ",\"runnerId\":" + runnerA + "}"))
                .andExpect(jsonPath("$.code").value(200));

        // 3. 第二人报名应被拒绝
        mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":" + eventId + ",\"runnerId\":" + runnerB + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("名额已满")));
    }

    @Test
    @DisplayName("报名参数缺失/非法时应失败")
    void shouldRejectSubmitWhenParamInvalid() throws Exception {
        // eventId 缺失 → body.get 为 null → Long.valueOf("null") 抛 NumberFormatException → code=500
        String body = "{\"runnerId\":1}";
        mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("正常取消报名应成功")
    void shouldCancelRegistrationSuccessfully() throws Exception {
        long runnerId = createFreshRunner("待取消报名选手");
        MvcResult submit = mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":1,\"runnerId\":" + runnerId + "}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long regId = objectMapper.readTree(submit.getResponse().getContentAsString())
                .path("data").path("registrationId").asLong();

        mockMvc.perform(withAuth(post("/api/register/cancel"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":" + regId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("业务校验：已取消的报名不能重复取消（答辩亮点）")
    void shouldRejectCancelTwice() throws Exception {
        long runnerId = createFreshRunner("双取消选手");
        MvcResult submit = mockMvc.perform(withAuth(post("/api/register/submit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":1,\"runnerId\":" + runnerId + "}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long regId = objectMapper.readTree(submit.getResponse().getContentAsString())
                .path("data").path("registrationId").asLong();

        // 第一次取消成功
        mockMvc.perform(withAuth(post("/api/register/cancel"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":" + regId + "}"))
                .andExpect(jsonPath("$.code").value(200));

        // 第二次取消应失败
        mockMvc.perform(withAuth(post("/api/register/cancel"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":" + regId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("不能重复取消")));
    }

    @Test
    @DisplayName("取消不存在的报名应失败")
    void shouldRejectCancelWhenNotFound() throws Exception {
        mockMvc.perform(withAuth(post("/api/register/cancel"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":99999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("报名记录不存在")));
    }

    @Test
    @DisplayName("正常分配号码布应成功")
    void shouldAssignBibSuccessfully() throws Exception {
        mockMvc.perform(withAuth(put("/api/register/bib"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":8,\"bibNumber\":\"B0099\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("B0099"));
    }

    @Test
    @DisplayName("分配号码布时报名不存在应失败")
    void shouldRejectAssignBibWhenNotFound() throws Exception {
        mockMvc.perform(withAuth(put("/api/register/bib"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":99999,\"bibNumber\":\"X0001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("报名记录不存在")));
    }

    @Test
    @DisplayName("分页第一页：名单含嵌套 runner / event")
    void shouldListFirstPageWithNestedObjects() throws Exception {
        mockMvc.perform(withAuth(get("/api/register/list"))
                        .param("pageNum", "1")
                        .param("pageSize", "5")
                        .param("eventId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.list", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.data.list[0].runner").exists())
                .andExpect(jsonPath("$.data.list[0].event").exists())
                .andExpect(jsonPath("$.data.list[0].event.eventName").isNotEmpty());
    }

    @Test
    @DisplayName("分页最后一页")
    void shouldListLastPage() throws Exception {
        mockMvc.perform(withAuth(get("/api/register/list"))
                        .param("pageNum", "2")
                        .param("pageSize", "5")
                        .param("eventId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("报名详情应返回嵌套 runner 与 event 对象")
    void shouldGetRegistrationDetailWithNestedObjects() throws Exception {
        mockMvc.perform(withAuth(get("/api/register/detail/{id}", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.runner.name").isNotEmpty())
                .andExpect(jsonPath("$.data.runner.phone").isNotEmpty())
                .andExpect(jsonPath("$.data.event.eventName").isNotEmpty())
                .andExpect(jsonPath("$.data.event.eventDate").isNotEmpty());
    }

    @Test
    @DisplayName("报名详情不存在应失败")
    void shouldFailWhenRegistrationDetailNotFound() throws Exception {
        mockMvc.perform(withAuth(get("/api/register/detail/{id}", 99999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("报名记录不存在")));
    }
}

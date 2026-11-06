package com.ape.marathon.controller;

import com.ape.marathon.entity.MarathonEvent;
import com.ape.marathon.mapper.MarathonEventMapper;
import com.ape.marathon.mapper.RegistrationMapper;
import com.ape.marathon.mapper.ResultMapper;
import com.ape.marathon.util.ExcelUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 导出接口
 * GET /api/register/export?eventId=1  → 报名名单
 * GET /api/result/export?eventId=1    → 成绩排名
 *
 * 说明：直接写 HttpServletResponse，不走统一 Result 包装（浏览器下载文件）
 */
@Api(tags = "Excel导出")
@RestController
public class ExportController {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private ResultMapper resultMapper;

    @Autowired
    private MarathonEventMapper eventMapper;

    /**
     * 导出某赛事报名名单
     * 列：序号、选手姓名、性别、手机号、号码布、报名时间、支付状态、报名状态
     */
    @ApiOperation("导出赛事报名名单 Excel")
    @GetMapping("/api/register/export")
    public void exportRegister(
            @ApiParam(value = "赛事ID", required = true) @RequestParam Long eventId,
            HttpServletResponse response) throws Exception {
        if (eventId == null) {
            throw new RuntimeException("赛事ID不能为空");
        }
        MarathonEvent event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new RuntimeException("赛事不存在");
        }

        List<Map<String, Object>> list = registrationMapper.selectExportList(eventId);
        String[] headers = {"序号", "选手姓名", "性别", "手机号", "号码布", "报名时间", "支付状态", "报名状态"};
        List<String[]> rows = new ArrayList<String[]>();
        int index = 1;
        for (Map<String, Object> m : list) {
            rows.add(new String[]{
                    String.valueOf(index++),
                    str(m, "runner_name"),
                    genderText(m.get("runner_gender")),
                    str(m, "runner_phone"),
                    str(m, "bib_number"),
                    str(m, "register_time"),
                    payText(m.get("pay_status")),
                    registerStatusText(m.get("status"))
            });
        }

        String fileName = safeName(event.getEventName()) + "_报名名单.xlsx";
        ExcelUtil.export(response, fileName, "报名名单", headers, rows);
    }

    /**
     * 导出某赛事成绩排名
     * 列：名次、选手姓名、号码布、性别、净成绩（mm:ss）、完赛状态
     */
    @ApiOperation("导出赛事成绩排名 Excel")
    @GetMapping("/api/result/export")
    public void exportResult(
            @ApiParam(value = "赛事ID", required = true) @RequestParam Long eventId,
            HttpServletResponse response) throws Exception {
        if (eventId == null) {
            throw new RuntimeException("赛事ID不能为空");
        }
        MarathonEvent event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new RuntimeException("赛事不存在");
        }

        List<Map<String, Object>> list = resultMapper.selectExportRanking(eventId);
        String[] headers = {"名次", "选手姓名", "号码布", "性别", "净成绩", "完赛状态"};
        List<String[]> rows = new ArrayList<String[]>();
        for (Map<String, Object> m : list) {
            rows.add(new String[]{
                    str(m, "rank"),
                    str(m, "name"),
                    str(m, "bib_number"),
                    genderText(m.get("gender")),
                    ExcelUtil.formatSecondsToMmSs(m.get("net_time")),
                    finishText(m.get("finish_status"))
            });
        }

        String fileName = safeName(event.getEventName()) + "_成绩排名.xlsx";
        ExcelUtil.export(response, fileName, "成绩排名", headers, rows);
    }

    // ---------- 辅助：Map 取值 / 状态文案 ----------

    private static String str(Map<String, Object> m, String key) {
        if (m == null) {
            return "";
        }
        Object v = m.get(key);
        if (v == null) {
            // 部分驱动可能返回大写 key
            v = m.get(key.toUpperCase());
        }
        return v == null ? "" : String.valueOf(v);
    }

    private static String genderText(Object gender) {
        if (gender == null) {
            return "";
        }
        String g = String.valueOf(gender);
        if ("1".equals(g)) {
            return "男";
        }
        if ("2".equals(g)) {
            return "女";
        }
        return g;
    }

    private static String payText(Object payStatus) {
        if (payStatus == null) {
            return "";
        }
        return "1".equals(String.valueOf(payStatus)) ? "已支付" : "未支付";
    }

    private static String registerStatusText(Object status) {
        if (status == null) {
            return "";
        }
        String s = String.valueOf(status);
        if ("1".equals(s)) {
            return "已报名";
        }
        if ("2".equals(s)) {
            return "已取消";
        }
        if ("3".equals(s)) {
            return "退赛";
        }
        return s;
    }

    private static String finishText(Object finishStatus) {
        if (finishStatus == null) {
            return "";
        }
        return "1".equals(String.valueOf(finishStatus)) ? "完赛" : "未完赛";
    }

    /** 文件名中去掉非法字符 */
    private static String safeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "赛事";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}

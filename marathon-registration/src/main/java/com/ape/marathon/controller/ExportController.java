package com.ape.marathon.controller;

import com.ape.marathon.api.dto.EventBrief;
import com.ape.marathon.api.feign.EventFeignClient;
import com.ape.marathon.common.Result;
import com.ape.marathon.mapper.RegistrationMapper;
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

@Api(tags = "Excel导出")
@RestController
public class ExportController {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private EventFeignClient eventFeignClient;

    @ApiOperation("导出赛事报名名单 Excel")
    @GetMapping("/api/register/export")
    public void exportRegister(
            @ApiParam(value = "赛事ID", required = true) @RequestParam Long eventId,
            HttpServletResponse response) throws Exception {
        if (eventId == null) {
            throw new RuntimeException("赛事ID不能为空");
        }
        Result<EventBrief> briefResult = eventFeignClient.getBrief(eventId);
        if (briefResult == null || !briefResult.ok() || briefResult.getData() == null) {
            throw new RuntimeException("赛事不存在");
        }
        EventBrief event = briefResult.getData();

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

    private static String str(Map<String, Object> m, String key) {
        if (m == null) {
            return "";
        }
        Object v = m.get(key);
        if (v == null) {
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

    private static String safeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "赛事";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}

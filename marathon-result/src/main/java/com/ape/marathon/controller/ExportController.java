package com.ape.marathon.controller;

import com.ape.marathon.api.dto.EventBrief;
import com.ape.marathon.api.feign.EventFeignClient;
import com.ape.marathon.common.Result;
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

@Api(tags = "Excel导出")
@RestController
public class ExportController {

    @Autowired
    private ResultMapper resultMapper;

    @Autowired
    private EventFeignClient eventFeignClient;

    @ApiOperation("导出赛事成绩排名 Excel")
    @GetMapping("/api/result/export")
    public void exportResult(
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

    private static String finishText(Object finishStatus) {
        if (finishStatus == null) {
            return "";
        }
        return "1".equals(String.valueOf(finishStatus)) ? "完赛" : "未完赛";
    }

    private static String safeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "赛事";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}

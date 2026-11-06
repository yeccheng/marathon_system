package com.ape.marathon.controller;

import com.ape.marathon.api.dto.EventBrief;
import com.ape.marathon.api.feign.EventFeignClient;
import com.ape.marathon.common.Result;
import com.ape.marathon.mapper.ResultPublicMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "公开成绩")
@RestController
@RequestMapping("/api/public")
public class PublicResultController {

    @Autowired
    private ResultPublicMapper resultPublicMapper;

    @Autowired
    private EventFeignClient eventFeignClient;

    @ApiOperation("公开：赛事成绩分布")
    @GetMapping("/event/{id}/stats")
    public Result<Map<String, Object>> eventStats(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long id) {
        requireEvent(id);
        Map<String, Object> raw = resultPublicMapper.selectEventFinishStats(id);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        int finishers = toInt(raw == null ? null : first(raw, "finishers", "finishers"));
        out.put("finishers", finishers);
        out.put("bestNet", first(raw, "bestNet", "best_net"));
        out.put("bestNetStr", secToClock(first(raw, "bestNet", "best_net")));
        out.put("avgNet", first(raw, "avgNet", "avg_net"));
        out.put("avgNetStr", secToClock(first(raw, "avgNet", "avg_net")));
        out.put("under3h", toInt(raw == null ? null : first(raw, "under3h", "under_3h")));
        out.put("h3to4", toInt(raw == null ? null : first(raw, "h3to4", "h3_to_4")));
        out.put("h4to5", toInt(raw == null ? null : first(raw, "h4to5", "h4_to_5")));
        out.put("over5h", toInt(raw == null ? null : first(raw, "over5h", "over_5h")));
        out.put("maleCount", toInt(raw == null ? null : first(raw, "maleCount", "male_count")));
        out.put("femaleCount", toInt(raw == null ? null : first(raw, "femaleCount", "female_count")));
        return Result.success(out);
    }

    @ApiOperation("公开：赛事成绩排名")
    @GetMapping("/rank/{eventId}")
    public Result<List<Map<String, Object>>> rank(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long eventId,
            @ApiParam(value = "号码布（模糊）") @RequestParam(required = false) String bibNumber,
            @ApiParam(value = "号码段前缀，如 A / A0 / B00") @RequestParam(required = false) String bibPrefix,
            @ApiParam(value = "省份代码（身份证前2位），如 61=陕西") @RequestParam(required = false) String province,
            @ApiParam(value = "姓名（模糊）") @RequestParam(required = false) String name,
            @ApiParam(value = "关键词（姓名或号码布）") @RequestParam(required = false) String keyword) {
        requireEvent(eventId);
        List<Map<String, Object>> list = resultPublicMapper.selectPublicRank(
                eventId,
                blankToNull(bibNumber),
                blankToNull(bibPrefix),
                blankToNull(province),
                blankToNull(name),
                blankToNull(keyword));
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : list) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            Object overall = first(row, "overallRank", "overall_rank");
            if (overall == null) {
                overall = first(row, "rank", "RANK");
            }
            item.put("rank", overall);
            item.put("overallRank", overall);
            item.put("genderRank", first(row, "genderRank", "gender_rank"));
            item.put("ageRank", first(row, "ageRank", "age_rank"));
            item.put("ageGroup", first(row, "ageGroup", "age_group"));
            Object nm = first(row, "runnerName", "runner_name");
            if (nm == null) {
                nm = row.get("name");
            }
            item.put("name", nm);
            item.put("gender", row.get("gender"));
            item.put("bibNumber", first(row, "bibNumber", "bib_number"));
            Object prov = first(row, "provinceCode", "province_code");
            item.put("provinceCode", prov);
            item.put("province", provinceName(prov));
            item.put("chipTime", first(row, "chipTime", "chip_time"));
            item.put("chipTimeStr", first(row, "chipTimeStr", "chip_time_str"));
            item.put("netTime", first(row, "netTime", "net_time"));
            item.put("netTimeStr", first(row, "netTimeStr", "net_time_str"));
            Object dist = first(row, "distanceKm", "distance_km");
            item.put("distanceKm", dist);
            item.put("pace", paceStr(first(row, "netTime", "net_time"), dist));
            out.add(item);
        }
        return Result.success(out);
    }

    private void requireEvent(Long eventId) {
        Result<EventBrief> r = eventFeignClient.getBrief(eventId);
        if (r == null || !r.ok() || r.getData() == null) {
            throw new RuntimeException("赛事不存在");
        }
    }

    private static String secToClock(Object secObj) {
        if (secObj == null) {
            return null;
        }
        int n;
        try {
            n = (int) Math.round(Double.parseDouble(String.valueOf(secObj)));
        } catch (Exception e) {
            return null;
        }
        if (n <= 0) {
            return null;
        }
        int h = n / 3600;
        int m = (n % 3600) / 60;
        int s = n % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static String provinceName(Object codeObj) {
        if (codeObj == null) {
            return null;
        }
        String code = String.valueOf(codeObj).trim();
        if (code.length() >= 2) {
            code = code.substring(0, 2);
        }
        switch (code) {
            case "11": return "北京";
            case "12": return "天津";
            case "13": return "河北";
            case "14": return "山西";
            case "15": return "内蒙古";
            case "21": return "辽宁";
            case "22": return "吉林";
            case "23": return "黑龙江";
            case "31": return "上海";
            case "32": return "江苏";
            case "33": return "浙江";
            case "34": return "安徽";
            case "35": return "福建";
            case "36": return "江西";
            case "37": return "山东";
            case "41": return "河南";
            case "42": return "湖北";
            case "43": return "湖南";
            case "44": return "广东";
            case "45": return "广西";
            case "46": return "海南";
            case "50": return "重庆";
            case "51": return "四川";
            case "52": return "贵州";
            case "53": return "云南";
            case "54": return "西藏";
            case "61": return "陕西";
            case "62": return "甘肃";
            case "63": return "青海";
            case "64": return "宁夏";
            case "65": return "新疆";
            default: return code.isEmpty() ? null : code;
        }
    }

    private static String paceStr(Object netTimeObj, Object distanceKmObj) {
        if (netTimeObj == null || distanceKmObj == null) {
            return null;
        }
        double dist;
        int net;
        try {
            dist = new BigDecimal(String.valueOf(distanceKmObj)).doubleValue();
            net = Integer.parseInt(String.valueOf(netTimeObj));
        } catch (Exception e) {
            return null;
        }
        if (dist <= 0 || net <= 0) {
            return null;
        }
        double secPerKm = net / dist;
        int totalSec = (int) Math.round(secPerKm);
        int m = totalSec / 60;
        int s = totalSec % 60;
        return m + "'" + String.format("%02d", s) + "/km";
    }

    private static Object first(Map<String, Object> m, String camel, String snake) {
        if (m == null) {
            return null;
        }
        if (m.containsKey(camel) && m.get(camel) != null) {
            return m.get(camel);
        }
        return m.get(snake);
    }

    private static int toInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

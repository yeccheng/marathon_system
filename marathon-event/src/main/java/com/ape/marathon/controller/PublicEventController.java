package com.ape.marathon.controller;

import com.ape.marathon.common.Result;
import com.ape.marathon.entity.MarathonEvent;
import com.ape.marathon.mapper.EventPublicMapper;
import com.ape.marathon.mapper.MarathonEventMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 选手端公开赛事接口（不含 stats，stats 在成绩服务）
 */
@Api(tags = "公开赛事")
@RestController
@RequestMapping("/api/public")
public class PublicEventController {

    @Autowired
    private EventPublicMapper eventPublicMapper;

    @Autowired
    private MarathonEventMapper eventMapper;

    @ApiOperation("公开：正在报名中的赛事列表")
    @GetMapping("/events")
    public Result<List<Map<String, Object>>> events() {
        return Result.success(mapEventCards(eventPublicMapper.selectPublicEvents()));
    }

    @ApiOperation("公开：全部赛事（含历史）")
    @GetMapping("/events/all")
    public Result<List<Map<String, Object>>> eventsAll() {
        return Result.success(mapEventCards(eventPublicMapper.selectAllEvents()));
    }

    @ApiOperation("公开：赛事详情")
    @GetMapping("/event/{id}")
    public Result<Map<String, Object>> eventDetail(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long id) {
        MarathonEvent e = eventMapper.selectById(id);
        if (e == null) {
            throw new RuntimeException("赛事不存在");
        }
        int quota = e.getQuota() == null ? 0 : e.getQuota();
        int reg = eventPublicMapper.countActiveRegs(id);
        eventMapper.syncRegisteredCount(id, reg);
        e = eventMapper.selectById(id);
        e.setRegisteredCount(reg);
        e.setRemainQuota(Math.max(quota - reg, 0));
        int finishers = eventPublicMapper.countFinishers(id);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", e.getId());
        data.put("eventName", e.getEventName());
        data.put("eventType", e.getEventType());
        data.put("eventDate", e.getEventDate());
        data.put("startLocation", e.getStartLocation());
        data.put("distanceKm", e.getDistanceKm());
        data.put("quota", e.getQuota());
        data.put("registeredCount", reg);
        data.put("remainQuota", e.getRemainQuota());
        data.put("entryFee", e.getEntryFee());
        data.put("status", e.getStatus());
        data.put("signupStart", e.getSignupStart());
        data.put("signupEnd", e.getSignupEnd());
        data.put("gunTime", e.getGunTime());
        data.put("cutOffTime", e.getCutOffTime());
        data.put("signupRule", e.getSignupRule());
        data.put("pickupInfo", e.getPickupInfo());
        data.put("routeDesc", e.getRouteDesc());
        data.put("aidStations", e.getAidStations());
        data.put("routeProfile", e.getRouteProfile());
        data.put("routeGpx", e.getRouteGpx());
        data.put("guideJson", e.getGuideJson());
        data.put("cancelRule", e.getCancelRule());
        data.put("finisherCount", finishers);
        data.put("hasResults", finishers > 0);
        data.put("phaseLabel", (e.getStatus() != null && e.getStatus() == 0)
                ? (finishers > 0 ? "报名中 · 成绩样例可查" : "报名中")
                : null);
        data.put("resultHint", finishers > 0
                ? ("当前展示成绩样例 " + finishers + " 条，便于体验查询与证书；正式比赛预计完赛约 "
                    + Math.max(quota / 2, finishers) + "–" + Math.max(quota, finishers)
                    + " 人，以比赛日芯片成绩为准。")
                : "成绩将于比赛日结束后约 2 小时起陆续公布，预计 24 小时内完整录入。");
        return Result.success(data);
    }

    private List<Map<String, Object>> mapEventCards(List<Map<String, Object>> list) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> e : list) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", e.get("id"));
            item.put("eventName", first(e, "eventName", "event_name"));
            item.put("eventType", first(e, "eventType", "event_type"));
            item.put("eventDate", first(e, "eventDate", "event_date"));
            item.put("startLocation", first(e, "startLocation", "start_location"));
            item.put("distanceKm", first(e, "distanceKm", "distance_km"));
            item.put("quota", e.get("quota"));
            int quota = toInt(e.get("quota"));
            int reg = toInt(first(e, "activeCount", "active_count"));
            item.put("registeredCount", reg);
            item.put("remainQuota", Math.max(quota - reg, 0));
            item.put("entryFee", first(e, "entryFee", "entry_fee"));
            item.put("status", e.get("status"));
            item.put("signupStart", first(e, "signupStart", "signup_start"));
            item.put("signupEnd", first(e, "signupEnd", "signup_end"));
            item.put("gunTime", first(e, "gunTime", "gun_time"));
            int finishers = toInt(first(e, "finisherCount", "finisher_count"));
            item.put("finisherCount", finishers);
            item.put("hasResults", finishers > 0);
            boolean open = "0".equals(String.valueOf(e.get("status")));
            item.put("phaseLabel", open
                    ? (finishers > 0 ? "报名中 · 成绩样例可查" : "报名中")
                    : null);
            out.add(item);
        }
        return out;
    }

    private static Object first(Map<String, Object> m, String camel, String snake) {
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
}

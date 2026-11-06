package com.ape.marathon.ai.tool;

import com.ape.marathon.ai.client.MarathonEventClient;
import com.ape.marathon.ai.dto.EventCard;
import com.ape.marathon.ai.dto.EventDetailView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 第一个可演示的 @Tool：查赛事信息（走主工程公开 API）。
 */
@Component
public class EventQueryTools {

    private final MarathonEventClient marathonEventClient;
    private final ObjectMapper objectMapper;

    public EventQueryTools(MarathonEventClient marathonEventClient, ObjectMapper objectMapper) {
        this.marathonEventClient = marathonEventClient;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "查询正在报名中的马拉松赛事列表。当用户问「有什么比赛」「还能报名吗」「近期赛事」时使用。")
    public String listOpenEvents() {
        return toJson(marathonEventClient.listOpenEvents());
    }

    @Tool(description = "按关键词搜索全部赛事（含历史）。关键词可匹配赛事名称或起点地点，例如「西安」「全程」「城墙」。")
    public String searchEvents(
            @ToolParam(description = "搜索关键词，可为空表示返回全部赛事摘要") String keyword) {
        List<EventCard> all = marathonEventClient.listAllEvents();
        if (keyword == null || keyword.isBlank()) {
            return toJson(limit(all, 12));
        }
        String q = keyword.trim().toLowerCase(Locale.ROOT);
        List<EventCard> matched = new ArrayList<>();
        for (EventCard e : all) {
            String name = e.getEventName() == null ? "" : e.getEventName().toLowerCase(Locale.ROOT);
            String loc = e.getStartLocation() == null ? "" : e.getStartLocation().toLowerCase(Locale.ROOT);
            String label = e.getPhaseLabel() == null ? "" : e.getPhaseLabel().toLowerCase(Locale.ROOT);
            if (name.contains(q) || loc.contains(q) || label.contains(q)) {
                matched.add(e);
            }
        }
        return toJson(limit(matched, 12));
    }

    @Tool(description = "按赛事 ID 查询单场赛事详情（报名费、剩余名额、地点、日期、规则等）。")
    public String getEventById(
            @ToolParam(description = "赛事数字 ID") Long eventId) {
        EventDetailView detail = marathonEventClient.getEventDetail(eventId);
        if (detail == null) {
            return "{\"error\":\"赛事不存在\",\"eventId\":" + eventId + "}";
        }
        return toJson(detail);
    }

    private List<EventCard> limit(List<EventCard> source, int max) {
        if (source.size() <= max) {
            return source;
        }
        return source.subList(0, max);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化工具结果失败", e);
        }
    }
}

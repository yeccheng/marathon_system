package com.ape.marathon.service.impl;

import com.ape.marathon.entity.MarathonEvent;
import com.ape.marathon.mapper.MarathonEventMapper;
import com.ape.marathon.mapper.RegistrationMapper;
import com.ape.marathon.mapper.ResultMapper;
import com.ape.marathon.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 统计看板实现：聚合报名、性别、完赛、退赛、平均净成绩等指标
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private MarathonEventMapper eventMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private ResultMapper resultMapper;

    @Override
    public Map<String, Object> eventOverview(Long eventId) {
        // 基础赛事信息（赛事名称）
        MarathonEvent event = eventMapper.selectById(eventId);
        Map<String, Object> overview = new HashMap<>();
        overview.put("eventId", eventId);
        overview.put("eventName", event == null ? "" : event.getEventName());

        // 报名人数（有效报名 status=1）
        long totalRegister = registrationMapper.countActiveByEvent(eventId);
        // 男女报名人数
        long maleCount = registrationMapper.countGenderByEvent(eventId, 1);
        long femaleCount = registrationMapper.countGenderByEvent(eventId, 2);
        // 完赛人数
        long finishCount = resultMapper.countFinishedByEvent(eventId);
        // 退赛人数（status=3）
        long retireCount = registrationMapper.countRetireByEvent(eventId);
        // 未完赛人数 = 报名数 - 完赛数 - 退赛数
        long unFinishCount = totalRegister - finishCount - retireCount;

        // 完赛率 = 完赛人数 / 报名人数 * 100（保留两位小数）
        double finishRate = totalRegister == 0 ? 0.0
                : Math.round(finishCount * 10000.0 / totalRegister) / 100.0;

        // 平均净成绩（秒）
        Long avgNetTime = resultMapper.avgNetTimeByEvent(eventId);

        overview.put("totalRegister", totalRegister);
        overview.put("maleCount", maleCount);
        overview.put("femaleCount", femaleCount);
        overview.put("finishCount", finishCount);
        overview.put("unFinishCount", unFinishCount);
        overview.put("retireCount", retireCount);
        overview.put("finishRate", finishRate);
        overview.put("avgNetTime", avgNetTime);
        // 将平均净成绩(秒)格式化为 "HH:mm:ss"
        overview.put("avgNetTimeStr", formatSeconds(avgNetTime));
        return overview;
    }

    /**
     * 将秒数格式化为 HH:mm:ss 字符串
     * @param seconds 总秒数，如 14500 -> "04:01:40"
     */
    private String formatSeconds(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return "00:00:00";
        }
        LocalTime time = LocalTime.ofSecondOfDay(seconds);
        return time.toString();
    }
}
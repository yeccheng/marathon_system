package com.ape.marathon.service.impl;

import com.ape.marathon.api.dto.EventBrief;
import com.ape.marathon.api.feign.EventFeignClient;
import com.ape.marathon.api.feign.RegistrationFeignClient;
import com.ape.marathon.common.Result;
import com.ape.marathon.mapper.ResultMapper;
import com.ape.marathon.mapper.RegistrationStatsMapper;
import com.ape.marathon.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 统计看板：赛事名/有效报名走 Feign，完赛与平均成绩走本地 ResultMapper；
 * 性别/退赛人数在共享库下用轻量 RegistrationStatsMapper。
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private EventFeignClient eventFeignClient;

    @Autowired
    private RegistrationFeignClient registrationFeignClient;

    @Autowired
    private ResultMapper resultMapper;

    @Autowired
    private RegistrationStatsMapper registrationStatsMapper;

    @Override
    public Map<String, Object> eventOverview(Long eventId) {
        String eventName = "";
        Result<EventBrief> eventResult = eventFeignClient.getBrief(eventId);
        if (eventResult != null && eventResult.ok() && eventResult.getData() != null) {
            eventName = eventResult.getData().getEventName();
        }

        long totalRegister = 0L;
        Result<Integer> countResult = registrationFeignClient.countActiveByEvent(eventId);
        if (countResult != null && countResult.ok() && countResult.getData() != null) {
            totalRegister = countResult.getData().longValue();
        }

        long maleCount = registrationStatsMapper.countGenderByEvent(eventId, 1);
        long femaleCount = registrationStatsMapper.countGenderByEvent(eventId, 2);
        long finishCount = resultMapper.countFinishedByEvent(eventId);
        long retireCount = registrationStatsMapper.countRetireByEvent(eventId);
        long unFinishCount = totalRegister - finishCount - retireCount;
        double finishRate = totalRegister == 0 ? 0.0
                : Math.round(finishCount * 10000.0 / totalRegister) / 100.0;
        Long avgNetTime = resultMapper.avgNetTimeByEvent(eventId);

        Map<String, Object> overview = new HashMap<>();
        overview.put("eventId", eventId);
        overview.put("eventName", eventName);
        overview.put("totalRegister", totalRegister);
        overview.put("maleCount", maleCount);
        overview.put("femaleCount", femaleCount);
        overview.put("finishCount", finishCount);
        overview.put("unFinishCount", unFinishCount);
        overview.put("retireCount", retireCount);
        overview.put("finishRate", finishRate);
        overview.put("avgNetTime", avgNetTime);
        overview.put("avgNetTimeStr", formatSeconds(avgNetTime));
        return overview;
    }

    private String formatSeconds(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return "00:00:00";
        }
        LocalTime time = LocalTime.ofSecondOfDay(seconds);
        return time.toString();
    }
}

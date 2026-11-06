package com.ape.marathon.service;

import java.util.Map;

/**
 * 统计看板业务接口
 */
public interface StatisticsService {

    /** 单场赛事统计总览：报名人数、男女占比、完赛率、平均完赛时间等 */
    Map<String, Object> eventOverview(Long eventId);
}
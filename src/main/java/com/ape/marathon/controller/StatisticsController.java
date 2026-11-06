package com.ape.marathon.controller;

import com.ape.marathon.common.Result;
import com.ape.marathon.service.StatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模块五：统计看板
 * 基础路径：/api/statistics
 */
@Api(tags = "统计看板")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /** 1. 单场赛事统计总览：GET /api/statistics/eventOverview/{eventId} */
    @ApiOperation("单场赛事统计总览")
    @GetMapping("/eventOverview/{eventId}")
    public Result<Map<String, Object>> eventOverview(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long eventId) {
        return Result.success(statisticsService.eventOverview(eventId));
    }
}

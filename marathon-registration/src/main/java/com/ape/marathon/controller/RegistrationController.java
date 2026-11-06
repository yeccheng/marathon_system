package com.ape.marathon.controller;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.common.Result;
import com.ape.marathon.service.RegistrationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模块三：赛事报名
 * 基础路径：/api/register
 */
@Api(tags = "赛事报名")
@RestController
@RequestMapping("/api/register")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    /** 1. 提交赛事报名：POST /api/register/submit，请求体 {eventId, runnerId} */
    @ApiOperation("提交赛事报名")
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(
            @ApiParam(value = "请求体，含 eventId、runnerId", required = true) @RequestBody Map<String, Object> body) {
        Long eventId = Long.valueOf(String.valueOf(body.get("eventId")));
        Long runnerId = Long.valueOf(String.valueOf(body.get("runnerId")));
        return Result.success(java.util.Collections.singletonMap("registrationId", registrationService.submit(eventId, runnerId)));
    }

    /** 2. 取消报名：POST /api/register/cancel，请求体 {registrationId} */
    @ApiOperation("取消报名")
    @PostMapping("/cancel")
    public Result<Boolean> cancel(
            @ApiParam(value = "请求体，含 registrationId", required = true) @RequestBody Map<String, Object> body) {
        Long registrationId = Long.valueOf(String.valueOf(body.get("registrationId")));
        return Result.success(registrationService.cancel(registrationId));
    }

    /** 3. 分配号码布：PUT /api/register/bib，请求体 {registrationId, bibNumber} */
    @ApiOperation("分配号码布")
    @PutMapping("/bib")
    public Result<String> assignBib(
            @ApiParam(value = "请求体，含 registrationId、bibNumber", required = true) @RequestBody Map<String, Object> body) {
        Long registrationId = Long.valueOf(String.valueOf(body.get("registrationId")));
        String bibNumber = String.valueOf(body.get("bibNumber"));
        return Result.success(registrationService.assignBib(registrationId, bibNumber));
    }

    /** 4. 分页查询报名名单：GET /api/register/list */
    @ApiOperation("分页查询报名名单")
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @ApiParam(value = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "每页条数", example = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "赛事ID") @RequestParam(required = false) Long eventId,
            @ApiParam(value = "选手姓名（模糊）") @RequestParam(required = false) String name) {
        return Result.success(registrationService.page(eventId, name, pageNum, pageSize));
    }

    /** 5. 查询单条报名详情：GET /api/register/detail/{id} */
    @ApiOperation("查询报名详情")
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(
            @ApiParam(value = "报名记录ID", required = true) @PathVariable Long id) {
        return Result.success(registrationService.detail(id));
    }
}

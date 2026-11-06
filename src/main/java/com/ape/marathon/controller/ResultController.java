package com.ape.marathon.controller;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.common.Result;
import com.ape.marathon.service.ResultService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 模块四：比赛成绩
 * 基础路径：/api/result
 */
@Api(tags = "比赛成绩")
@RestController
@RequestMapping("/api/result")
public class ResultController {

    @Autowired
    private ResultService resultService;


    /** 1. 录入选手完赛成绩：POST /api/result/input（一条报名记录只能录一次） */
    @ApiOperation("录入选手完赛成绩")
    @PostMapping("/input")
    public Result<java.util.Map<String, Object>> input(@Valid @RequestBody com.ape.marathon.entity.Result result,
                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(bindingResult.getFieldError().getDefaultMessage());
        }
        return Result.success(java.util.Collections.singletonMap("id", resultService.input(result)));
    }

    /** 2. 修改成绩信息：PUT /api/result/update */
    @ApiOperation("修改成绩信息")
    @PutMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody com.ape.marathon.entity.Result result,
                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(bindingResult.getFieldError().getDefaultMessage());
        }
        return Result.success(resultService.update(result));
    }

    /** 3. 赛事总排名：GET /api/result/eventRanking/{eventId}（按净成绩升序） */
    @ApiOperation("赛事总排名（按净成绩升序）")
    @GetMapping("/eventRanking/{eventId}")
    public Result<PageResult<Map<String, Object>>> eventRanking(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long eventId,
            @ApiParam(value = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "每页条数", example = "10") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(resultService.eventRanking(eventId, pageNum, pageSize));
    }

    /** 4. 男女分组排名：GET /api/result/genderRanking/{eventId}（返回 maleList / femaleList） */
    @ApiOperation("男女分组排名")
    @GetMapping("/genderRanking/{eventId}")
    public Result<Map<String, Object>> genderRanking(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long eventId,
            @ApiParam(value = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "每页条数", example = "10") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(resultService.genderRanking(eventId, pageNum, pageSize));
    }

    /** 5. 年龄分组排名：GET /api/result/ageRanking/{eventId}（≤29 / 30-39 / 40-49 / ≥50） */
    @ApiOperation("年龄分组排名")
    @GetMapping("/ageRanking/{eventId}")
    public Result<Map<String, Object>> ageRanking(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long eventId,
            @ApiParam(value = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "每页条数", example = "10") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(resultService.ageRanking(eventId, pageNum, pageSize));
    }

    /** 6. 分页查询成绩记录：GET /api/result/list */
    @ApiOperation("分页查询成绩记录")
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @ApiParam(value = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "每页条数", example = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "赛事ID") @RequestParam(required = false) Long eventId) {
        return Result.success(resultService.page(eventId, pageNum, pageSize));
    }
}

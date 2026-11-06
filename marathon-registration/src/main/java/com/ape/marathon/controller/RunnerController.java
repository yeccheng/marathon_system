package com.ape.marathon.controller;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.common.Result;
import com.ape.marathon.entity.Runner;
import com.ape.marathon.service.RunnerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 模块二：选手管理
 * 基础路径：/api/runner
 */
@Api(tags = "选手管理")
@RestController
@RequestMapping("/api/runner")
public class RunnerController {

    @Autowired
    private RunnerService runnerService;

    /** 1. 新增选手档案：POST /api/runner/add */
    @ApiOperation("新增选手档案")
    @PostMapping("/add")
    public Result<java.util.Map<String, Object>> add(@Valid @RequestBody Runner runner,
                                                     BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(bindingResult.getFieldError().getDefaultMessage());
        }
        return Result.success(java.util.Collections.singletonMap("id", runnerService.add(runner)));
    }

    /** 2. 修改选手档案：PUT /api/runner/update */
    @ApiOperation("修改选手档案")
    @PutMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody Runner runner,
                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(bindingResult.getFieldError().getDefaultMessage());
        }
        return Result.success(runnerService.update(runner));
    }

    /** 3. 分页查询选手列表（姓名/手机号模糊）：GET /api/runner/list */
    @ApiOperation("分页查询选手列表")
    @GetMapping("/list")
    public Result<PageResult<Runner>> list(
            @ApiParam(value = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "每页条数", example = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "姓名（模糊）") @RequestParam(required = false) String name,
            @ApiParam(value = "手机号（模糊）") @RequestParam(required = false) String phone) {
        return Result.success(runnerService.page(name, phone, pageNum, pageSize));
    }

    /** 4. 查询选手详情：GET /api/runner/detail/{id} */
    @ApiOperation("查询选手详情")
    @GetMapping("/detail/{id}")
    public Result<Runner> detail(
            @ApiParam(value = "选手ID", required = true) @PathVariable Long id) {
        return Result.success(runnerService.detail(id));
    }

    /** 5. 删除选手（有报名记录的选手不能删除）：DELETE /api/runner/delete/{id} */
    @ApiOperation("删除选手（有报名记录不可删）")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(
            @ApiParam(value = "选手ID", required = true) @PathVariable Long id) {
        return Result.success(runnerService.delete(id));
    }
}

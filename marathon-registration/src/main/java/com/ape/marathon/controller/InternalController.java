package com.ape.marathon.controller;

import com.ape.marathon.api.dto.RegistrationBrief;
import com.ape.marathon.common.Result;
import com.ape.marathon.entity.Registration;
import com.ape.marathon.entity.Runner;
import com.ape.marathon.mapper.RegistrationMapper;
import com.ape.marathon.mapper.RunnerMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 报名内部接口（供赛事/成绩服务 Feign 调用）
 */
@Api(tags = "报名内部接口")
@RestController
@RequestMapping("/internal/registration")
public class InternalController {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @ApiOperation("内部：报名摘要")
    @GetMapping("/{id}")
    public Result<RegistrationBrief> getBrief(@PathVariable("id") Long id) {
        Registration reg = registrationMapper.selectById(id);
        if (reg == null) {
            return Result.fail("报名记录不存在");
        }
        RegistrationBrief brief = new RegistrationBrief();
        brief.setId(reg.getId());
        brief.setEventId(reg.getEventId());
        brief.setRunnerId(reg.getRunnerId());
        brief.setBibNumber(reg.getBibNumber());
        brief.setStatus(reg.getStatus());
        brief.setPayStatus(reg.getPayStatus());
        Runner runner = runnerMapper.selectById(reg.getRunnerId());
        if (runner != null) {
            brief.setRunnerName(runner.getName());
            brief.setGender(runner.getGender());
            if (runner.getBirthDate() != null) {
                brief.setBirthDate(runner.getBirthDate().toString());
            }
        }
        return Result.success(brief);
    }

    @ApiOperation("内部：有效报名人数")
    @GetMapping("/count")
    public Result<Integer> countActiveByEvent(@RequestParam("eventId") Long eventId) {
        return Result.success((int) registrationMapper.countActiveByEvent(eventId));
    }

    @ApiOperation("内部：赛事是否存在报名")
    @GetMapping("/exists")
    public Result<Boolean> existsByEvent(@RequestParam("eventId") Long eventId) {
        return Result.success(registrationMapper.countAnyByEvent(eventId) > 0);
    }

    @ApiOperation("内部：选手是否存在报名")
    @GetMapping("/exists-by-runner")
    public Result<Boolean> existsByRunner(@RequestParam("runnerId") Long runnerId) {
        return Result.success(registrationMapper.countByRunner(runnerId) > 0);
    }
}

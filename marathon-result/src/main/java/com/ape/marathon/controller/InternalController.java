package com.ape.marathon.controller;

import com.ape.marathon.common.Result;
import com.ape.marathon.mapper.ResultMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "成绩内部接口")
@RestController
@RequestMapping("/internal/result")
public class InternalController {

    @Autowired
    private ResultMapper resultMapper;

    @ApiOperation("内部：完赛人数")
    @GetMapping("/finishers")
    public Result<Integer> countFinishers(@RequestParam("eventId") Long eventId) {
        return Result.success((int) resultMapper.countFinishedByEvent(eventId));
    }
}

package com.ape.marathon.controller;

import com.ape.marathon.api.dto.EventBrief;
import com.ape.marathon.common.Result;
import com.ape.marathon.entity.MarathonEvent;
import com.ape.marathon.mapper.MarathonEventMapper;
import com.ape.marathon.service.RedisService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 赛事内部接口（供报名/成绩等服务 Feign 调用）
 */
@Api(tags = "赛事内部接口")
@RestController
@RequestMapping("/internal/event")
public class InternalController {

    private static final String CACHE_PREFIX_EVENT_PAGE = "event:page:";

    @Autowired
    private MarathonEventMapper eventMapper;

    @Autowired
    private RedisService redisService;

    @ApiOperation("内部：赛事摘要")
    @GetMapping("/{id}")
    public Result<EventBrief> getBrief(@PathVariable("id") Long id) {
        MarathonEvent e = eventMapper.selectById(id);
        if (e == null) {
            return Result.fail("赛事不存在");
        }
        EventBrief brief = toBrief(e);
        return Result.success(brief);
    }

    @ApiOperation("内部：报名人数 +1")
    @PostMapping("/{id}/quota/increase")
    public Result<Boolean> increaseQuota(@PathVariable("id") Long id) {
        if (eventMapper.selectById(id) == null) {
            return Result.fail("赛事不存在");
        }
        boolean ok = eventMapper.increaseRegisteredCount(id) > 0;
        if (ok) {
            redisService.deleteByPrefix(CACHE_PREFIX_EVENT_PAGE);
        }
        return Result.success(ok);
    }

    @ApiOperation("内部：报名人数 -1")
    @PostMapping("/{id}/quota/decrease")
    public Result<Boolean> decreaseQuota(@PathVariable("id") Long id) {
        if (eventMapper.selectById(id) == null) {
            return Result.fail("赛事不存在");
        }
        boolean ok = eventMapper.decreaseRegisteredCount(id) > 0;
        if (ok) {
            redisService.deleteByPrefix(CACHE_PREFIX_EVENT_PAGE);
        }
        return Result.success(ok);
    }

    @ApiOperation("内部：按有效报名数回写 registered_count")
    @PostMapping("/{id}/quota/sync")
    public Result<Boolean> syncQuota(@PathVariable("id") Long id,
                                     @RequestParam("activeCount") int activeCount) {
        if (eventMapper.selectById(id) == null) {
            return Result.fail("赛事不存在");
        }
        boolean ok = eventMapper.syncRegisteredCount(id, activeCount) > 0;
        if (ok) {
            redisService.deleteByPrefix(CACHE_PREFIX_EVENT_PAGE);
        }
        return Result.success(ok);
    }

    private static EventBrief toBrief(MarathonEvent e) {
        EventBrief b = new EventBrief();
        b.setId(e.getId());
        b.setEventName(e.getEventName());
        b.setEventType(e.getEventType());
        b.setEventDate(e.getEventDate());
        b.setStartLocation(e.getStartLocation());
        b.setDistanceKm(e.getDistanceKm());
        b.setQuota(e.getQuota());
        b.setRegisteredCount(e.getRegisteredCount());
        int remain = (e.getQuota() == null ? 0 : e.getQuota())
                - (e.getRegisteredCount() == null ? 0 : e.getRegisteredCount());
        b.setRemainQuota(Math.max(remain, 0));
        b.setEntryFee(e.getEntryFee());
        b.setStatus(e.getStatus());
        b.setSignupStart(e.getSignupStart());
        b.setSignupEnd(e.getSignupEnd());
        b.setCancelRule(e.getCancelRule());
        return b;
    }
}

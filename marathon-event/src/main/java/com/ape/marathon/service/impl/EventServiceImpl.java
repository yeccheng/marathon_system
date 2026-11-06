package com.ape.marathon.service.impl;

import com.ape.marathon.api.feign.RegistrationFeignClient;
import com.ape.marathon.common.PageResult;
import com.ape.marathon.common.Result;
import com.ape.marathon.entity.MarathonEvent;
import com.ape.marathon.mapper.MarathonEventMapper;
import com.ape.marathon.service.EventService;
import com.ape.marathon.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 赛事业务实现：删除前通过 Feign 校验报名记录；分页列表走 Redis 缓存
 */
@Service
public class EventServiceImpl implements EventService {

    private static final String CACHE_PREFIX_EVENT_PAGE = "event:page:";

    @Autowired
    private MarathonEventMapper eventMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RegistrationFeignClient registrationFeignClient;

    @Override
    public Long add(MarathonEvent event) {
        if (event.getRegisteredCount() == null) {
            event.setRegisteredCount(0);
        }
        eventMapper.insert(event);
        redisService.deleteByPrefix(CACHE_PREFIX_EVENT_PAGE);
        return event.getId();
    }

    @Override
    public boolean update(MarathonEvent event) {
        boolean ok = eventMapper.updateById(event) > 0;
        if (ok) {
            redisService.deleteByPrefix(CACHE_PREFIX_EVENT_PAGE);
        }
        return ok;
    }

    @Override
    public PageResult<MarathonEvent> page(String eventName, Integer eventType, Integer status,
                                          String startDate, String endDate,
                                          int pageNum, int pageSize) {
        String cacheKey = buildPageCacheKey(eventName, eventType, status, startDate, endDate, pageNum, pageSize);
        PageResult<MarathonEvent> cached = redisService.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int offset = (pageNum - 1) * pageSize;
        java.util.List<MarathonEvent> list =
                eventMapper.selectPage(eventName, eventType, status, startDate, endDate, offset, pageSize);
        long total = eventMapper.countForPage(eventName, eventType, status, startDate, endDate);
        PageResult<MarathonEvent> pageResult = PageResult.of(total, pageNum, pageSize, list);
        boolean empty = list == null || list.isEmpty() || total == 0;
        redisService.setWithSmartTtl(cacheKey, pageResult, empty);
        return pageResult;
    }

    @Override
    public MarathonEvent detail(Long id) {
        MarathonEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw new RuntimeException("赛事不存在");
        }
        int remain = (event.getQuota() == null ? 0 : event.getQuota())
                - (event.getRegisteredCount() == null ? 0 : event.getRegisteredCount());
        event.setRemainQuota(Math.max(remain, 0));
        return event;
    }

    @Override
    public boolean delete(Long id) {
        MarathonEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw new RuntimeException("赛事不存在");
        }
        Result<Boolean> exists = registrationFeignClient.existsByEvent(id);
        if (exists != null && exists.ok() && Boolean.TRUE.equals(exists.getData())) {
            throw new RuntimeException("该赛事已有报名记录，不能删除");
        }
        if (event.getStatus() != null && event.getStatus() != 0) {
            throw new RuntimeException("该赛事已开始或已结束，不能删除");
        }
        boolean ok = eventMapper.deleteById(id) > 0;
        if (ok) {
            redisService.deleteByPrefix(CACHE_PREFIX_EVENT_PAGE);
        }
        return ok;
    }

    private String buildPageCacheKey(String eventName, Integer eventType, Integer status,
                                     String startDate, String endDate,
                                     int pageNum, int pageSize) {
        return CACHE_PREFIX_EVENT_PAGE
                + pageNum + ":"
                + pageSize + ":"
                + nullToEmpty(eventName) + ":"
                + nullToEmpty(eventType) + ":"
                + nullToEmpty(status) + ":"
                + nullToEmpty(startDate) + ":"
                + nullToEmpty(endDate);
    }

    private static String nullToEmpty(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}

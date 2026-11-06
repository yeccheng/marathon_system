package com.ape.marathon.service.impl;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.entity.MarathonEvent;
import com.ape.marathon.mapper.MarathonEventMapper;
import com.ape.marathon.service.EventService;
import com.ape.marathon.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 赛事业务实现：含剩余名额计算、删除前置校验
 * 分页列表走 Redis 缓存；增删改后清空 event:page:*
 */
@Service
public class EventServiceImpl implements EventService {

    /** 赛事分页缓存前缀 */
    private static final String CACHE_PREFIX_EVENT_PAGE = "event:page:";

    @Autowired
    private MarathonEventMapper eventMapper;

    @Autowired
    private RedisService redisService;

    @Override
    public Long add(MarathonEvent event) {
        // 新赛事默认已报名人数为 0
        if (event.getRegisteredCount() == null) {
            event.setRegisteredCount(0);
        }
        eventMapper.insert(event);
        // 缓存失效：新增赛事后，所有分页列表条件可能变化，清空 event:page:*
        redisService.deleteByPrefix(CACHE_PREFIX_EVENT_PAGE);
        // MyBatis 已通过 useGeneratedKeys 将自增 id 回填到 event.id
        return event.getId();
    }

    @Override
    public boolean update(MarathonEvent event) {
        boolean ok = eventMapper.updateById(event) > 0;
        if (ok) {
            // 缓存失效：修改赛事信息后清空列表缓存
            redisService.deleteByPrefix(CACHE_PREFIX_EVENT_PAGE);
        }
        return ok;
    }

    @Override
    public PageResult<MarathonEvent> page(String eventName, Integer eventType, Integer status,
                                          String startDate, String endDate,
                                          int pageNum, int pageSize) {
        // 1. 拼缓存 key：event:page:pageNum:pageSize:eventName:eventType:status:startDate:endDate
        String cacheKey = buildPageCacheKey(eventName, eventType, status, startDate, endDate, pageNum, pageSize);

        // 2. 先读 Redis
        PageResult<MarathonEvent> cached = redisService.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 3. 未命中则查库
        int offset = (pageNum - 1) * pageSize;
        java.util.List<MarathonEvent> list =
                eventMapper.selectPage(eventName, eventType, status, startDate, endDate, offset, pageSize);
        long total = eventMapper.countForPage(eventName, eventType, status, startDate, endDate);
        PageResult<MarathonEvent> pageResult = PageResult.of(total, pageNum, pageSize, list);

        // 4. 写入缓存：有数据 10 分钟；空结果 1 分钟（防缓存穿透）
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
        // 计算剩余名额 = 总名额 - 已报名人数
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
        // 校验1：已有报名记录的赛事不允许删除
        if (eventMapper.countRegistrationByEventId(id) > 0) {
            throw new RuntimeException("该赛事已有报名记录，不能删除");
        }
        // 校验2：仅未开始（状态0报名中）的赛事允许删除
        if (event.getStatus() != null && event.getStatus() != 0) {
            throw new RuntimeException("该赛事已开始或已结束，不能删除");
        }
        boolean ok = eventMapper.deleteById(id) > 0;
        if (ok) {
            // 缓存失效：删除赛事后清空列表缓存
            redisService.deleteByPrefix(CACHE_PREFIX_EVENT_PAGE);
        }
        return ok;
    }

    /**
     * 组装赛事分页缓存 key（null 参数用空串占位，保证同条件命中同一 key）
     */
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

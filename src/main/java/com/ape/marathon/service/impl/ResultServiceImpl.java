package com.ape.marathon.service.impl;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.entity.Result;
import com.ape.marathon.mapper.RegistrationMapper;
import com.ape.marathon.mapper.ResultMapper;
import com.ape.marathon.service.RedisService;
import com.ape.marathon.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成绩业务实现：成绩唯一校验、总/男女/年龄分组排名
 * 总排名走 Redis 缓存；录入/修改成绩后清空 result:rank:{eventId}:*
 */
@Service
public class ResultServiceImpl implements ResultService {

    /** 成绩排名缓存前缀：result:rank:{eventId}:{pageNum}:{pageSize} */
    private static final String CACHE_PREFIX_RESULT_RANK = "result:rank:";

    @Autowired
    private ResultMapper resultMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private RedisService redisService;

    @Override
    public Long input(Result result) {
        // 校验报名记录存在
        Map<String, Object> reg = registrationMapper.selectDetailById(result.getRegistrationId());
        if (reg == null) {
            throw new RuntimeException("报名记录不存在，无法录入成绩");
        }
        // 校验一条报名记录只能录入一次成绩（数据库唯一约束兜底）
        if (resultMapper.selectByRegistration(result.getRegistrationId()) != null) {
            throw new RuntimeException("该报名记录已录入成绩，不能重复录入");
        }
        resultMapper.insert(result);

        // 缓存失效：录入成绩影响该赛事排名，清空 result:rank:{eventId}:*
        Long eventId = toLong(reg.get("event_id"));
        if (eventId != null) {
            redisService.deleteByPrefix(CACHE_PREFIX_RESULT_RANK + eventId + ":");
        }
        return result.getId();
    }

    @Override
    public boolean update(Result result) {
        // 先解析所属赛事（更新前查库，避免字段不全）
        Long eventId = resolveEventId(result);
        boolean ok = resultMapper.updateById(result) > 0;
        if (ok && eventId != null) {
            // 缓存失效：修改成绩后清空该赛事排名缓存
            redisService.deleteByPrefix(CACHE_PREFIX_RESULT_RANK + eventId + ":");
        }
        return ok;
    }

    @Override
    public PageResult<Map<String, Object>> eventRanking(Long eventId, int pageNum, int pageSize) {
        // 1. 缓存 key：result:rank:eventId:pageNum:pageSize
        String cacheKey = CACHE_PREFIX_RESULT_RANK + eventId + ":" + pageNum + ":" + pageSize;

        // 2. 先读 Redis
        PageResult<Map<String, Object>> cached = redisService.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 3. 未命中查库
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> list = resultMapper.eventRanking(eventId, offset, pageSize);
        // 总排名记录数 = 该赛事完赛人数
        long total = resultMapper.countFinishedByEvent(eventId);
        PageResult<Map<String, Object>> pageResult = PageResult.of(total, pageNum, pageSize, list);

        // 4. 写入缓存：有数据 10 分钟；空结果 1 分钟（防穿透，如恶意刷不存在赛事）
        boolean empty = list == null || list.isEmpty() || total == 0;
        redisService.setWithSmartTtl(cacheKey, pageResult, empty);
        return pageResult;
    }

    @Override
    public Map<String, Object> genderRanking(Long eventId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Map<String, Object> result = new HashMap<>();
        // 男子组（gender=1）
        result.put("maleList", resultMapper.genderRanking(eventId, 1, offset, pageSize));
        // 女子组（gender=2）
        result.put("femaleList", resultMapper.genderRanking(eventId, 2, offset, pageSize));
        return result;
    }

    @Override
    public Map<String, Object> ageRanking(Long eventId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Map<String, Object> result = new HashMap<>();
        // 年龄段条件（对应文档：≤29 / 30-39 / 40-49 / ≥50）
        result.put("group_29below", resultMapper.ageRanking(eventId, "TIMESTAMPDIFF(YEAR, ru.birth_date, CURDATE()) <= 29", offset, pageSize));
        result.put("group_30_39",   resultMapper.ageRanking(eventId, "TIMESTAMPDIFF(YEAR, ru.birth_date, CURDATE()) BETWEEN 30 AND 39", offset, pageSize));
        result.put("group_40_49",   resultMapper.ageRanking(eventId, "TIMESTAMPDIFF(YEAR, ru.birth_date, CURDATE()) BETWEEN 40 AND 49", offset, pageSize));
        result.put("group_50up",    resultMapper.ageRanking(eventId, "TIMESTAMPDIFF(YEAR, ru.birth_date, CURDATE()) >= 50", offset, pageSize));
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> page(Long eventId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> list = resultMapper.selectPage(eventId, offset, pageSize);
        long total = resultMapper.countForPage(eventId);
        return PageResult.of(total, pageNum, pageSize, list);
    }

    /**
     * 根据成绩对象解析所属赛事 ID（优先用入参 registrationId，否则按成绩 id 查库）
     */
    private Long resolveEventId(Result result) {
        if (result == null) {
            return null;
        }
        Long registrationId = result.getRegistrationId();
        if (registrationId == null && result.getId() != null) {
            Result db = resultMapper.selectById(result.getId());
            if (db != null) {
                registrationId = db.getRegistrationId();
            }
        }
        if (registrationId == null) {
            return null;
        }
        Map<String, Object> reg = registrationMapper.selectDetailById(registrationId);
        return reg == null ? null : toLong(reg.get("event_id"));
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

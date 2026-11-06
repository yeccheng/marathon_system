package com.ape.marathon.service.impl;

import com.ape.marathon.api.dto.RegistrationBrief;
import com.ape.marathon.api.feign.RegistrationFeignClient;
import com.ape.marathon.common.PageResult;
import com.ape.marathon.common.Result;
import com.ape.marathon.mapper.ResultMapper;
import com.ape.marathon.service.RedisService;
import com.ape.marathon.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成绩业务：录入前通过 RegistrationFeignClient 校验报名
 */
@Service
public class ResultServiceImpl implements ResultService {

    private static final String CACHE_PREFIX_RESULT_RANK = "result:rank:";

    @Autowired
    private ResultMapper resultMapper;

    @Autowired
    private RegistrationFeignClient registrationFeignClient;

    @Autowired
    private RedisService redisService;

    @Override
    public Long input(com.ape.marathon.entity.Result result) {
        RegistrationBrief reg = requireRegistration(result.getRegistrationId());
        if (resultMapper.selectByRegistration(result.getRegistrationId()) != null) {
            throw new RuntimeException("该报名记录已录入成绩，不能重复录入");
        }
        resultMapper.insert(result);
        Long eventId = reg.getEventId();
        if (eventId != null) {
            redisService.deleteByPrefix(CACHE_PREFIX_RESULT_RANK + eventId + ":");
        }
        return result.getId();
    }

    @Override
    public boolean update(com.ape.marathon.entity.Result result) {
        Long eventId = resolveEventId(result);
        boolean ok = resultMapper.updateById(result) > 0;
        if (ok && eventId != null) {
            redisService.deleteByPrefix(CACHE_PREFIX_RESULT_RANK + eventId + ":");
        }
        return ok;
    }

    @Override
    public PageResult<Map<String, Object>> eventRanking(Long eventId, int pageNum, int pageSize) {
        String cacheKey = CACHE_PREFIX_RESULT_RANK + eventId + ":" + pageNum + ":" + pageSize;
        PageResult<Map<String, Object>> cached = redisService.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> list = resultMapper.eventRanking(eventId, offset, pageSize);
        long total = resultMapper.countFinishedByEvent(eventId);
        PageResult<Map<String, Object>> pageResult = PageResult.of(total, pageNum, pageSize, list);
        boolean empty = list == null || list.isEmpty() || total == 0;
        redisService.setWithSmartTtl(cacheKey, pageResult, empty);
        return pageResult;
    }

    @Override
    public Map<String, Object> genderRanking(Long eventId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Map<String, Object> result = new HashMap<>();
        result.put("maleList", resultMapper.genderRanking(eventId, 1, offset, pageSize));
        result.put("femaleList", resultMapper.genderRanking(eventId, 2, offset, pageSize));
        return result;
    }

    @Override
    public Map<String, Object> ageRanking(Long eventId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Map<String, Object> result = new HashMap<>();
        result.put("group_29below", resultMapper.ageRanking(eventId, "TIMESTAMPDIFF(YEAR, ru.birth_date, CURDATE()) <= 29", offset, pageSize));
        result.put("group_30_39", resultMapper.ageRanking(eventId, "TIMESTAMPDIFF(YEAR, ru.birth_date, CURDATE()) BETWEEN 30 AND 39", offset, pageSize));
        result.put("group_40_49", resultMapper.ageRanking(eventId, "TIMESTAMPDIFF(YEAR, ru.birth_date, CURDATE()) BETWEEN 40 AND 49", offset, pageSize));
        result.put("group_50up", resultMapper.ageRanking(eventId, "TIMESTAMPDIFF(YEAR, ru.birth_date, CURDATE()) >= 50", offset, pageSize));
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> page(Long eventId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> list = resultMapper.selectPage(eventId, offset, pageSize);
        long total = resultMapper.countForPage(eventId);
        return PageResult.of(total, pageNum, pageSize, list);
    }

    private RegistrationBrief requireRegistration(Long registrationId) {
        Result<RegistrationBrief> r = registrationFeignClient.getBrief(registrationId);
        if (r == null || !r.ok() || r.getData() == null) {
            throw new RuntimeException("报名记录不存在，无法录入成绩");
        }
        return r.getData();
    }

    private Long resolveEventId(com.ape.marathon.entity.Result result) {
        if (result == null) {
            return null;
        }
        Long registrationId = result.getRegistrationId();
        if (registrationId == null && result.getId() != null) {
            com.ape.marathon.entity.Result db = resultMapper.selectById(result.getId());
            if (db != null) {
                registrationId = db.getRegistrationId();
            }
        }
        if (registrationId == null) {
            return null;
        }
        try {
            return requireRegistration(registrationId).getEventId();
        } catch (RuntimeException e) {
            return null;
        }
    }
}

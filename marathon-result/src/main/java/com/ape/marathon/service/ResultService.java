package com.ape.marathon.service;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.entity.Result;
import java.util.List;
import java.util.Map;

/**
 * 比赛成绩业务接口
 */
public interface ResultService {

    /** 录入成绩：一条报名记录只能录入一次 */
    Long input(Result result);

    /** 修改成绩 */
    boolean update(Result result);

    /** 赛事总排名（按净成绩升序） */
    PageResult<Map<String, Object>> eventRanking(Long eventId, int pageNum, int pageSize);

    /** 男女分组排名：返回 maleList / femaleList 两个数组 */
    Map<String, Object> genderRanking(Long eventId, int pageNum, int pageSize);

    /** 年龄分组排名：≤29 / 30-39 / 40-49 / ≥50 */
    Map<String, Object> ageRanking(Long eventId, int pageNum, int pageSize);

    /** 分页查询成绩记录 */
    PageResult<Map<String, Object>> page(Long eventId, int pageNum, int pageSize);
}
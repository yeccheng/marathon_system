package com.ape.marathon.service;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.entity.MarathonEvent;

/**
 * 赛事业务接口
 */
public interface EventService {

    /** 新增赛事，返回生成的主键 id */
    Long add(MarathonEvent event);

    /** 修改赛事信息 */
    boolean update(MarathonEvent event);

    /** 分页多条件查询赛事列表 */
    PageResult<MarathonEvent> page(String eventName, Integer eventType, Integer status,
                                   String startDate, String endDate,
                                   int pageNum, int pageSize);

    /** 查询赛事详情（附带剩余名额 remainQuota） */
    MarathonEvent detail(Long id);

    /** 删除赛事（校验：无报名记录且未开始） */
    boolean delete(Long id);
}
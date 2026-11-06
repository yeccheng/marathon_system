package com.ape.marathon.service;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.entity.Runner;

/**
 * 选手业务接口
 */
public interface RunnerService {

    /** 新增选手档案 */
    Long add(Runner runner);

    /** 修改选手档案 */
    boolean update(Runner runner);

    /** 分页查询选手列表（姓名/手机号模糊） */
    PageResult<Runner> page(String name, String phone, int pageNum, int pageSize);

    /** 查询选手详情 */
    Runner detail(Long id);

    /** 删除选手（校验：无报名记录） */
    boolean delete(Long id);
}
package com.ape.marathon.service.impl;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.entity.Runner;
import com.ape.marathon.mapper.RegistrationMapper;
import com.ape.marathon.mapper.RunnerMapper;
import com.ape.marathon.service.RunnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 选手业务：删除前用本服务 RegistrationMapper 校验报名记录
 */
@Service
public class RunnerServiceImpl implements RunnerService {

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Override
    public Long add(Runner runner) {
        runnerMapper.insert(runner);
        return runner.getId();
    }

    @Override
    public boolean update(Runner runner) {
        return runnerMapper.updateById(runner) > 0;
    }

    @Override
    public PageResult<Runner> page(String name, String phone, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        java.util.List<Runner> list = runnerMapper.selectPage(name, phone, offset, pageSize);
        long total = runnerMapper.countForPage(name, phone);
        return PageResult.of(total, pageNum, pageSize, list);
    }

    @Override
    public Runner detail(Long id) {
        Runner runner = runnerMapper.selectById(id);
        if (runner == null) {
            throw new RuntimeException("选手不存在");
        }
        return runner;
    }

    @Override
    public boolean delete(Long id) {
        Runner runner = runnerMapper.selectById(id);
        if (runner == null) {
            throw new RuntimeException("选手不存在");
        }
        if (registrationMapper.countByRunner(id) > 0) {
            throw new RuntimeException("该选手已有报名记录，不能删除");
        }
        return runnerMapper.deleteById(id) > 0;
    }
}

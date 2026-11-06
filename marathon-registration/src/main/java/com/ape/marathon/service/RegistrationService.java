package com.ape.marathon.service;

import com.ape.marathon.common.PageResult;
import java.util.Map;

/**
 * 报名业务接口
 */
public interface RegistrationService {

    /** 提交报名：校验赛事状态、名额、防重复，成功后同步赛事报名人数（默认已支付，供管理端） */
    Long submit(Long eventId, Long runnerId);

    /**
     * 提交报名并指定支付状态与尺码
     * @param payStatus 0 未支付 / 1 已支付（公开报名应传 0）
     * @param shirtSize 参赛服尺码，可空
     */
    Long submit(Long eventId, Long runnerId, Integer payStatus, String shirtSize);

    /** 收银台确认支付：写渠道/凭证/payment_log，返回报名摘要 */
    Map<String, Object> confirmPay(Long registrationId, String channel);

    /** 跑者自助退赛（校验赛前 7 天窗口）；已支付则生成可追踪退款单 */
    Map<String, Object> runnerCancel(Long registrationId, Long runnerId);

    /** 取消报名：扣减赛事报名人数 */
    boolean cancel(Long registrationId);

    /** 分配号码布 */
    String assignBib(Long registrationId, String bibNumber);

    /** 分页查询报名名单（可关联选手与赛事信息） */
    PageResult<Map<String, Object>> page(Long eventId, String name, int pageNum, int pageSize);

    /** 查询单条报名详情（附带选手与赛事信息） */
    Map<String, Object> detail(Long id);
}
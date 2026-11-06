package com.ape.marathon.service.impl;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.config.PayProperties;
import com.ape.marathon.entity.MarathonEvent;
import com.ape.marathon.entity.Registration;
import com.ape.marathon.mapper.MarathonEventMapper;
import com.ape.marathon.mapper.NoticeMapper;
import com.ape.marathon.mapper.PaymentLogMapper;
import com.ape.marathon.mapper.RegistrationMapper;
import com.ape.marathon.mapper.RunnerMapper;
import com.ape.marathon.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 报名业务：名额/防重复、收银台支付、可追踪退款、取消扣减人数
 */
@Service
public class RegistrationServiceImpl implements RegistrationService {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private MarathonEventMapper eventMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private PaymentLogMapper paymentLogMapper;

    @Autowired
    private PayProperties payProperties;

    @Override
    public Long submit(Long eventId, Long runnerId) {
        return submit(eventId, runnerId, 1, null);
    }

    @Override
    public Long submit(Long eventId, Long runnerId, Integer payStatus, String shirtSize) {
        MarathonEvent event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new RuntimeException("赛事不存在");
        }
        if (event.getStatus() != null && event.getStatus() != 0) {
            throw new RuntimeException("该赛事当前状态不允许报名");
        }
        int registered = event.getRegisteredCount() == null ? 0 : event.getRegisteredCount();
        int quota = event.getQuota() == null ? 0 : event.getQuota();
        if (registered >= quota) {
            throw new RuntimeException("赛事名额已满，报名失败");
        }
        if (runnerMapper.selectById(runnerId) == null) {
            throw new RuntimeException("选手不存在");
        }
        if (registrationMapper.selectByEventAndRunner(eventId, runnerId) != null) {
            throw new RuntimeException("该选手已报名此赛事，请勿重复报名");
        }
        int pay = (payStatus != null && payStatus == 1) ? 1 : 0;
        Registration registration = new Registration();
        registration.setEventId(eventId);
        registration.setRunnerId(runnerId);
        registration.setShirtSize(blankToNull(shirtSize));
        registration.setOrderNo(genOrderNo());
        registration.setPayStatus(pay);
        registration.setStatus(1);
        registrationMapper.insert(registration);
        eventMapper.increaseRegisteredCount(eventId);
        eventMapper.syncRegisteredCount(eventId);

        BigDecimal fee = event.getEntryFee() == null ? BigDecimal.ZERO : event.getEntryFee();
        paymentLogMapper.insert(registration.getId(), registration.getOrderNo(), "create",
                null, fee, null, "报名订单已创建，待收银台支付");

        noticeMapper.insert(runnerId, "报名已登记",
                "订单号 " + registration.getOrderNo() + " 已生成。请前往收银台完成支付以确认报名；号码布将于赛前通过站内通知发放。");
        return registration.getId();
    }

    @Override
    public Map<String, Object> confirmPay(Long registrationId, String channel) {
        Map<String, Object> detail = registrationMapper.selectDetailById(registrationId);
        if (detail == null) {
            throw new RuntimeException("报名记录不存在");
        }
        Object status = detail.get("status");
        if (status != null && !"1".equals(String.valueOf(status))) {
            throw new RuntimeException("该报名已取消，无法支付");
        }
        String payChannel = normalizeChannel(channel);
        Object pay = detail.get("pay_status");
        String voucher;
        String orderNo = detail.get("order_no") == null ? null : String.valueOf(detail.get("order_no"));
        BigDecimal amount = toMoney(detail.get("entry_fee"));

        if (pay != null && "1".equals(String.valueOf(pay))) {
            Registration existing = registrationMapper.selectById(registrationId);
            voucher = existing == null ? null : existing.getPayVoucher();
            payChannel = existing != null && existing.getPayChannel() != null
                    ? existing.getPayChannel() : payChannel;
        } else if (pay != null && "2".equals(String.valueOf(pay))) {
            throw new RuntimeException("该订单已退款，无法再次支付");
        } else {
            voucher = genVoucher(payChannel);
            registrationMapper.markPaid(registrationId, voucher, payChannel);
            paymentLogMapper.insert(registrationId, orderNo, "pay", payChannel, amount, voucher,
                    "支付成功 · " + channelLabel(payChannel));
            Long runnerId = Long.valueOf(String.valueOf(detail.get("runner_id")));
            noticeMapper.insert(runnerId, "支付成功",
                    "订单 " + orderNo + " 已支付成功（" + channelLabel(payChannel) + "），凭证 "
                            + voucher + "。请在个人中心下载电子确认函；号码布预计赛前 7–14 天发放。");
        }
        Map<String, Object> nested = detail(registrationId);
        nested.put("payVoucher", voucher);
        nested.put("payChannel", payChannel);
        nested.put("payChannelLabel", channelLabel(payChannel));
        nested.put("merchantName", payProperties.getMerchantName());
        nested.put("gateway", resolveGatewayLabel(payChannel));
        nested.put("amount", amount);
        return nested;
    }

    @Override
    public Map<String, Object> runnerCancel(Long registrationId, Long runnerId) {
        Registration reg = registrationMapper.selectById(registrationId);
        if (reg == null) {
            throw new RuntimeException("报名记录不存在");
        }
        if (!runnerId.equals(reg.getRunnerId())) {
            throw new RuntimeException("无权操作该报名");
        }
        if (reg.getStatus() == null || reg.getStatus() != 1) {
            throw new RuntimeException("该报名已取消或已退赛");
        }
        MarathonEvent event = eventMapper.selectById(reg.getEventId());
        if (event == null || event.getEventDate() == null) {
            throw new RuntimeException("赛事信息不完整，无法退赛");
        }
        LocalDate deadline = event.getEventDate().minusDays(7);
        if (!LocalDate.now().isBefore(deadline)) {
            throw new RuntimeException("已过在线退赛窗口（须在比赛日前 7 天之前操作）。规则："
                    + (event.getCancelRule() == null ? "请联系组委会" : event.getCancelRule()));
        }

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("registrationId", registrationId);
        out.put("orderNo", reg.getOrderNo());

        Integer payStatus = reg.getPayStatus() == null ? 0 : reg.getPayStatus();
        if (payStatus == 1) {
            BigDecimal amount = event.getEntryFee() == null ? BigDecimal.ZERO : event.getEntryFee();
            String refundNo = genRefundNo();
            String channel = reg.getPayChannel() == null ? "mock" : reg.getPayChannel();
            registrationMapper.markRefunded(registrationId, refundNo, amount);
            eventMapper.decreaseRegisteredCount(reg.getEventId());
            eventMapper.syncRegisteredCount(reg.getEventId());
            paymentLogMapper.insert(registrationId, reg.getOrderNo(), "refund", channel, amount, refundNo,
                    "原路退回 · " + channelLabel(channel) + " · 退款成功");
            noticeMapper.insert(runnerId, "退赛退款已完成",
                    "报名 #" + registrationId + " 已取消。退款单号 " + refundNo + "，金额 ¥" + amount
                            + "，原路退回「" + channelLabel(channel) + "」。可在个人中心查看退款状态。");
            out.put("refunded", true);
            out.put("refundNo", refundNo);
            out.put("refundAmount", amount);
            out.put("refundStatus", 1);
            out.put("payChannel", channel);
            out.put("message", "退赛已受理，退款单 " + refundNo + " 已原路退回 ¥" + amount);
        } else {
            registrationMapper.cancel(registrationId);
            eventMapper.decreaseRegisteredCount(reg.getEventId());
            eventMapper.syncRegisteredCount(reg.getEventId());
            paymentLogMapper.insert(registrationId, reg.getOrderNo(), "cancel", null, BigDecimal.ZERO, null,
                    "未支付订单取消，无退款");
            noticeMapper.insert(runnerId, "退赛已受理",
                    "报名 #" + registrationId + " 已取消，名额已释放（未支付，无退款）。");
            out.put("refunded", false);
            out.put("refundNo", null);
            out.put("refundAmount", null);
            out.put("refundStatus", 0);
            out.put("message", "退赛已受理（未支付订单，无退款）");
        }
        return out;
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.trim().isEmpty()) {
            return "mock";
        }
        String c = channel.trim().toLowerCase();
        if ("wechat".equals(c) || "alipay".equals(c) || "mock".equals(c)) {
            return c;
        }
        throw new RuntimeException("不支持的支付渠道，请选择微信 / 支付宝 / 模拟网关");
    }

    private String resolveGatewayLabel(String channel) {
        String gw = payProperties.getGateway() == null ? "mock" : payProperties.getGateway().trim().toLowerCase();
        if ("wechat".equals(channel) && payProperties.getWechatMchId() != null
                && !payProperties.getWechatMchId().trim().isEmpty()) {
            return "wechat-mch:" + payProperties.getWechatMchId().trim();
        }
        if ("alipay".equals(channel) && payProperties.getAlipayAppId() != null
                && !payProperties.getAlipayAppId().trim().isEmpty()) {
            return "alipay-app:" + payProperties.getAlipayAppId().trim();
        }
        if ("mock".equals(gw) || "mock".equals(channel)) {
            return "mock-cashier";
        }
        return gw + "-fallback-mock";
    }

    private static String channelLabel(String channel) {
        if ("wechat".equals(channel)) return "微信支付";
        if ("alipay".equals(channel)) return "支付宝";
        return "模拟收银台";
    }

    private static BigDecimal toMoney(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static String genOrderNo() {
        return "GS" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
    }

    private static String genVoucher(String channel) {
        String prefix = "wechat".equals(channel) ? "WX" : ("alipay".equals(channel) ? "ZFB" : "PV");
        return prefix + System.currentTimeMillis()
                + String.format("%03d", ThreadLocalRandom.current().nextInt(0, 1000));
    }

    private static String genRefundNo() {
        return "RF" + System.currentTimeMillis()
                + String.format("%03d", ThreadLocalRandom.current().nextInt(0, 1000));
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t.toUpperCase();
    }

    @Override
    public boolean cancel(Long registrationId) {
        Map<String, Object> detail = registrationMapper.selectDetailById(registrationId);
        if (detail == null) {
            throw new RuntimeException("报名记录不存在");
        }
        Object status = detail.get("status");
        if (status != null && !"1".equals(String.valueOf(status))) {
            throw new RuntimeException("该报名已取消或已退赛，不能重复取消");
        }
        registrationMapper.cancel(registrationId);
        Object eventIdObj = detail.get("event_id");
        if (eventIdObj != null) {
            eventMapper.decreaseRegisteredCount(Long.valueOf(String.valueOf(eventIdObj)));
        }
        return true;
    }

    @Override
    public String assignBib(Long registrationId, String bibNumber) {
        Map<String, Object> detail = registrationMapper.selectDetailById(registrationId);
        if (detail == null) {
            throw new RuntimeException("报名记录不存在");
        }
        registrationMapper.updateBib(registrationId, bibNumber);
        Long runnerId = Long.valueOf(String.valueOf(detail.get("runner_id")));
        noticeMapper.insert(runnerId, "号码布已发放",
                "您的号码布为 " + bibNumber + "，请在个人中心与领物时核对。本通知替代短信提醒。");
        return bibNumber;
    }

    @Override
    public PageResult<Map<String, Object>> page(Long eventId, String name, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> raw = registrationMapper.selectPage(eventId, name, offset, pageSize);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> m : raw) {
            list.add(buildNested(m, false));
        }
        long total = registrationMapper.countForPage(eventId, name);
        return PageResult.of(total, pageNum, pageSize, list);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        Map<String, Object> raw = registrationMapper.selectDetailById(id);
        if (raw == null) {
            throw new RuntimeException("报名记录不存在");
        }
        return buildNested(raw, true);
    }

    private Map<String, Object> buildNested(Map<String, Object> m, boolean fullEvent) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", m.get("id"));
        result.put("eventId", m.get("event_id"));
        result.put("runnerId", m.get("runner_id"));
        result.put("bibNumber", m.get("bib_number"));
        result.put("shirtSize", m.get("shirt_size"));
        result.put("orderNo", m.get("order_no"));
        result.put("registerTime", m.get("register_time"));
        result.put("payStatus", m.get("pay_status"));
        result.put("payChannel", m.get("pay_channel"));
        result.put("payTime", m.get("pay_time"));
        result.put("payVoucher", m.get("pay_voucher"));
        result.put("refundStatus", m.get("refund_status"));
        result.put("refundNo", m.get("refund_no"));
        result.put("refundAmount", m.get("refund_amount"));
        result.put("refundTime", m.get("refund_time"));
        result.put("status", m.get("status"));

        Map<String, Object> runner = new LinkedHashMap<>();
        runner.put("id", m.get("runner_id"));
        runner.put("name", m.get("runner_name"));
        runner.put("gender", m.get("runner_gender"));
        runner.put("phone", m.get("runner_phone"));
        result.put("runner", runner);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventName", m.get("event_name"));
        if (fullEvent) {
            event.put("id", m.get("event_id"));
            event.put("eventDate", m.get("event_date"));
            event.put("entryFee", m.get("entry_fee"));
        }
        result.put("event", event);
        return result;
    }
}

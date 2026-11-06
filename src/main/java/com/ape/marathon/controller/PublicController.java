package com.ape.marathon.controller;

import com.ape.marathon.common.Result;
import com.ape.marathon.entity.MarathonEvent;
import com.ape.marathon.entity.Runner;
import com.ape.marathon.mapper.MarathonEventMapper;
import com.ape.marathon.mapper.NoticeMapper;
import com.ape.marathon.mapper.PublicMapper;
import com.ape.marathon.mapper.RunnerMapper;
import com.ape.marathon.service.RedisService;
import com.ape.marathon.service.RegistrationService;
import com.ape.marathon.service.SmsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 选手端公开接口（不需要登录）
 * 路径前缀：/api/public
 * Shiro：/api/public/** = anon
 */
@Api(tags = "选手端公开接口")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private PublicMapper publicMapper;

    @Autowired
    private MarathonEventMapper eventMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private SmsService smsService;

    /** 1. 选手端首页：正在报名中的赛事列表 */
    @ApiOperation("公开：正在报名中的赛事列表")
    @GetMapping("/events")
    public Result<List<Map<String, Object>>> events() {
        return Result.success(mapEventCards(publicMapper.selectPublicEvents()));
    }

    /** 1.1 全部赛事（含已截止/已结束，方便查历史成绩与公开榜） */
    @ApiOperation("公开：全部赛事（含历史）")
    @GetMapping("/events/all")
    public Result<List<Map<String, Object>>> eventsAll() {
        return Result.success(mapEventCards(publicMapper.selectAllEvents()));
    }

    private List<Map<String, Object>> mapEventCards(List<Map<String, Object>> list) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> e : list) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", e.get("id"));
            item.put("eventName", first(e, "eventName", "event_name"));
            item.put("eventType", first(e, "eventType", "event_type"));
            item.put("eventDate", first(e, "eventDate", "event_date"));
            item.put("startLocation", first(e, "startLocation", "start_location"));
            item.put("distanceKm", first(e, "distanceKm", "distance_km"));
            item.put("quota", e.get("quota"));
            // 一律用有效报名实时人数，避免 registered_count 漂移导致各页名额不一致
            int quota = toInt(e.get("quota"));
            int reg = toInt(first(e, "activeCount", "active_count"));
            item.put("registeredCount", reg);
            item.put("remainQuota", Math.max(quota - reg, 0));
            item.put("entryFee", first(e, "entryFee", "entry_fee"));
            item.put("status", e.get("status"));
            item.put("signupStart", first(e, "signupStart", "signup_start"));
            item.put("signupEnd", first(e, "signupEnd", "signup_end"));
            item.put("gunTime", first(e, "gunTime", "gun_time"));
            int finishers = toInt(first(e, "finisherCount", "finisher_count"));
            item.put("finisherCount", finishers);
            item.put("hasResults", finishers > 0);
            // 报名中但已有成绩：标明为「成绩样例/往届参考」，避免赛前赛后时空打架
            boolean open = "0".equals(String.valueOf(e.get("status")));
            item.put("phaseLabel", open
                    ? (finishers > 0 ? "报名中 · 成绩样例可查" : "报名中")
                    : null);
            out.add(item);
        }
        return out;
    }

    /** 2. 赛事详情 */
    @ApiOperation("公开：赛事详情")
    @GetMapping("/event/{id}")
    public Result<Map<String, Object>> eventDetail(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long id) {
        MarathonEvent e = eventMapper.selectById(id);
        if (e == null) {
            throw new RuntimeException("赛事不存在");
        }
        eventMapper.syncRegisteredCount(id);
        e = eventMapper.selectById(id);
        int quota = e.getQuota() == null ? 0 : e.getQuota();
        int reg = publicMapper.countActiveRegs(id);
        e.setRegisteredCount(reg);
        e.setRemainQuota(Math.max(quota - reg, 0));
        int finishers = publicMapper.countFinishers(id);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", e.getId());
        data.put("eventName", e.getEventName());
        data.put("eventType", e.getEventType());
        data.put("eventDate", e.getEventDate());
        data.put("startLocation", e.getStartLocation());
        data.put("distanceKm", e.getDistanceKm());
        data.put("quota", e.getQuota());
        data.put("registeredCount", reg);
        data.put("remainQuota", e.getRemainQuota());
        data.put("entryFee", e.getEntryFee());
        data.put("status", e.getStatus());
        data.put("signupStart", e.getSignupStart());
        data.put("signupEnd", e.getSignupEnd());
        data.put("gunTime", e.getGunTime());
        data.put("cutOffTime", e.getCutOffTime());
        data.put("signupRule", e.getSignupRule());
        data.put("pickupInfo", e.getPickupInfo());
        data.put("routeDesc", e.getRouteDesc());
        data.put("aidStations", e.getAidStations());
        data.put("routeProfile", e.getRouteProfile());
        data.put("routeGpx", e.getRouteGpx());
        data.put("guideJson", e.getGuideJson());
        data.put("cancelRule", e.getCancelRule());
        data.put("finisherCount", finishers);
        data.put("hasResults", finishers > 0);
        data.put("phaseLabel", (e.getStatus() != null && e.getStatus() == 0)
                ? (finishers > 0 ? "报名中 · 成绩样例可查" : "报名中")
                : null);
        data.put("resultHint", finishers > 0
                ? ("当前展示成绩样例 " + finishers + " 条，便于体验查询与证书；正式比赛预计完赛约 "
                    + Math.max(quota / 2, finishers) + "–" + Math.max(quota, finishers)
                    + " 人，以比赛日芯片成绩为准。")
                : "成绩将于比赛日结束后约 2 小时起陆续公布，预计 24 小时内完整录入。");
        return Result.success(data);
    }

    /** 2.1 赛事完赛成绩分布 */
    @ApiOperation("公开：赛事成绩分布")
    @GetMapping("/event/{id}/stats")
    public Result<Map<String, Object>> eventStats(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long id) {
        if (eventMapper.selectById(id) == null) {
            throw new RuntimeException("赛事不存在");
        }
        Map<String, Object> raw = publicMapper.selectEventFinishStats(id);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        int finishers = toInt(raw == null ? null : first(raw, "finishers", "finishers"));
        out.put("finishers", finishers);
        out.put("bestNet", first(raw, "bestNet", "best_net"));
        out.put("bestNetStr", secToClock(first(raw, "bestNet", "best_net")));
        out.put("avgNet", first(raw, "avgNet", "avg_net"));
        out.put("avgNetStr", secToClock(first(raw, "avgNet", "avg_net")));
        out.put("under3h", toInt(raw == null ? null : first(raw, "under3h", "under_3h")));
        out.put("h3to4", toInt(raw == null ? null : first(raw, "h3to4", "h3_to_4")));
        out.put("h4to5", toInt(raw == null ? null : first(raw, "h4to5", "h4_to_5")));
        out.put("over5h", toInt(raw == null ? null : first(raw, "over5h", "over_5h")));
        out.put("maleCount", toInt(raw == null ? null : first(raw, "maleCount", "male_count")));
        out.put("femaleCount", toInt(raw == null ? null : first(raw, "femaleCount", "female_count")));
        return Result.success(out);
    }

    private static String secToClock(Object secObj) {
        if (secObj == null) {
            return null;
        }
        int n;
        try {
            n = (int) Math.round(Double.parseDouble(String.valueOf(secObj)));
        } catch (Exception e) {
            return null;
        }
        if (n <= 0) {
            return null;
        }
        int h = n / 3600;
        int m = (n % 3600) / 60;
        int s = n % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /**
     * 2.5 公开报名：填档案 → 建/复用选手 → 提交报名
     * body: eventId, name, gender, idCard, phone, birthDate, emergencyContact
     */
    @ApiOperation("公开：我要报名")
    @PostMapping("/signup")
    public Result<Map<String, Object>> signup(@RequestBody Map<String, Object> body) {
        if (body == null) {
            throw new RuntimeException("请求体不能为空");
        }
        Long eventId = toLong(body.get("eventId"));
        if (eventId == null) {
            throw new RuntimeException("请选择赛事");
        }
        String name = str(body.get("name"));
        String idCard = str(body.get("idCard"));
        String phone = str(body.get("phone"));
        String birthDate = str(body.get("birthDate"));
        String emergency = str(body.get("emergencyContact"));
        String emName = str(body.get("emergencyName"));
        String emPhone = str(body.get("emergencyPhone"));
        if (!emName.isEmpty() || !emPhone.isEmpty()) {
            if (emName.isEmpty() || !emPhone.matches("^1[3-9]\\d{9}$")) {
                throw new RuntimeException("请分别填写紧急联系人姓名与 11 位手机号");
            }
            emergency = emName + " " + emPhone;
        }
        String shirtSize = str(body.get("shirtSize")).toUpperCase();
        Integer gender = body.get("gender") == null ? null : toInt(body.get("gender"));

        if (name.isEmpty()) {
            throw new RuntimeException("请填写姓名");
        }
        if (gender == null || (gender != 1 && gender != 2)) {
            throw new RuntimeException("请选择性别");
        }
        if (!idCard.matches("^[1-9]\\d{16}[\\dXx]$")) {
            throw new RuntimeException("身份证号须为 18 位");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        // 出生日期可手填；未填时尝试从身份证号第 7–14 位解析
        if (birthDate.isEmpty() && idCard.length() >= 14) {
            birthDate = idCard.substring(6, 10) + "-" + idCard.substring(10, 12) + "-" + idCard.substring(12, 14);
        }
        if (birthDate.isEmpty()) {
            throw new RuntimeException("请填写出生日期（或填写正确身份证号以便自动识别）");
        }
        try {
            LocalDate.parse(birthDate);
        } catch (Exception ex) {
            throw new RuntimeException("出生日期格式应为 yyyy-MM-dd");
        }
        if (emergency.isEmpty()) {
            throw new RuntimeException("请填写紧急联系人");
        }
        if (!shirtSize.matches("^(XS|S|M|L|XL|XXL)$")) {
            throw new RuntimeException("请选择参赛服尺码（XS/S/M/L/XL/XXL）");
        }

        MarathonEvent event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new RuntimeException("赛事不存在");
        }
        if (event.getStatus() == null || event.getStatus() != 0) {
            throw new RuntimeException("该赛事当前不可报名");
        }

        Runner byPhone = runnerMapper.selectByPhone(phone);
        Runner byCard = runnerMapper.selectByIdCard(idCard);
        if (byPhone != null && byCard != null && !byPhone.getId().equals(byCard.getId())) {
            throw new RuntimeException("这个手机号和身份证分别对应两份不同档案："
                    + "手机号已登记给「" + maskName(byPhone.getName()) + "」，"
                    + "身份证已登记给「" + maskName(byCard.getName()) + "」。"
                    + "请任选其一：①改用该手机号原档案上的身份证；"
                    + "②改用该身份证原档案上的手机号；"
                    + "③换一组全新的手机号+身份证重新报名。");
        }

        Runner runner = byPhone != null ? byPhone : byCard;
        if (runner == null) {
            runner = new Runner();
            runner.setName(name);
            runner.setGender(gender);
            runner.setIdCard(idCard);
            runner.setPhone(phone);
            runner.setBirthDate(LocalDate.parse(birthDate));
            runner.setEmergencyContact(emergency);
            runnerMapper.insert(runner);
        } else {
            // 轻微同步可改字段，身份证/手机号不覆盖以免冲突
            runner.setName(name);
            runner.setGender(gender);
            runner.setBirthDate(LocalDate.parse(birthDate));
            runner.setEmergencyContact(emergency);
            if (runner.getPhone() == null || runner.getPhone().trim().isEmpty()) {
                runner.setPhone(phone);
            }
            runnerMapper.updateById(runner);
        }

        Long registrationId = registrationService.submit(eventId, runner.getId(), 0, shirtSize);
        Map<String, Object> regDetail = registrationService.detail(registrationId);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("registrationId", registrationId);
        data.put("runnerId", runner.getId());
        data.put("eventId", eventId);
        data.put("eventName", event.getEventName());
        data.put("eventDate", event.getEventDate());
        data.put("entryFee", event.getEntryFee());
        data.put("phone", phone);
        data.put("name", runner.getName());
        data.put("shirtSize", shirtSize);
        data.put("orderNo", regDetail.get("orderNo"));
        data.put("payStatus", 0);
        data.put("cancelRule", event.getCancelRule());
        data.put("bibHint", bibHint(event.getEventDate() == null ? null : event.getEventDate().toString()));
        return Result.success(data);
    }

    /**
     * 2.6 收银台确认支付
     * body: { registrationId, phone, channel: wechat|alipay|mock }
     */
    @ApiOperation("公开：收银台确认支付")
    @PostMapping("/confirmPay")
    public Result<Map<String, Object>> confirmPay(@RequestBody Map<String, Object> body) {
        if (body == null) {
            throw new RuntimeException("请求体不能为空");
        }
        Long registrationId = toLong(body.get("registrationId"));
        String phone = str(body.get("phone"));
        String channel = str(body.get("channel"));
        if (registrationId == null) {
            throw new RuntimeException("缺少报名号");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        Map<String, Object> detail = registrationService.detail(registrationId);
        Map<?, ?> runnerMap = detail.get("runner") instanceof Map ? (Map<?, ?>) detail.get("runner") : null;
        Object phoneObj = runnerMap == null ? null : runnerMap.get("phone");
        if (phoneObj == null || !phone.equals(String.valueOf(phoneObj).trim())) {
            throw new RuntimeException("手机号与报名记录不匹配");
        }
        Map<String, Object> refreshed = registrationService.confirmPay(registrationId, channel);
        Map<?, ?> ev = refreshed.get("event") instanceof Map ? (Map<?, ?>) refreshed.get("event") : null;
        Map<?, ?> ru = refreshed.get("runner") instanceof Map ? (Map<?, ?>) refreshed.get("runner") : null;
        Object eventDate = ev == null ? null : ev.get("eventDate");
        Object entryFee = refreshed.get("amount");
        if (entryFee == null && ev != null) {
            entryFee = ev.get("entryFee");
        }
        Map<String, Object> letter = new LinkedHashMap<String, Object>();
        letter.put("registrationId", registrationId);
        letter.put("orderNo", refreshed.get("orderNo"));
        letter.put("payVoucher", refreshed.get("payVoucher"));
        letter.put("payChannel", refreshed.get("payChannel"));
        letter.put("payChannelLabel", refreshed.get("payChannelLabel"));
        letter.put("merchantName", refreshed.get("merchantName"));
        letter.put("gateway", refreshed.get("gateway"));
        letter.put("entryFee", entryFee);
        letter.put("payStatus", 1);
        letter.put("shirtSize", refreshed.get("shirtSize"));
        letter.put("status", refreshed.get("status"));
        letter.put("bibNumber", refreshed.get("bibNumber"));
        letter.put("eventName", ev == null ? null : ev.get("eventName"));
        letter.put("eventDate", eventDate);
        letter.put("name", ru == null ? null : ru.get("name"));
        letter.put("phone", ru == null ? null : ru.get("phone"));
        letter.put("bibHint", bibHint(eventDate == null ? null : String.valueOf(eventDate)));
        letter.put("message", "支付成功（" + refreshed.get("payChannelLabel") + "）。请保存订单号与支付凭证，并可下载电子确认函；号码布将通过站内通知发放。");
        return Result.success(letter);
    }

    /** 发送登录验证码：可配置短信通道，写入 sms_log + 站内通知 */
    @ApiOperation("公开：发送手机验证码")
    @PostMapping("/otp/send")
    public Result<Map<String, Object>> sendOtp(@RequestBody Map<String, Object> body) {
        String phone = str(body == null ? null : body.get("phone"));
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
        redisService.set("public:otp:" + phone, code, 5, TimeUnit.MINUTES);
        String noticeTitle = "登录验证码";
        String noticeContent = "您的登录验证码是 " + code + "，5 分钟内有效。";
        Map<String, Object> sms = smsService.send(phone, "otp_login", noticeContent, noticeTitle);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("expireSeconds", 300);
        data.put("hint", "验证码已发送，5 分钟内有效");
        boolean expose = Boolean.TRUE.equals(sms.get("exposeCode"));
        if (expose) {
            data.put("code", code);
        }
        return Result.success(data);
    }

    /** 验证码登录个人中心，返回 runnerToken */
    @ApiOperation("公开：验证码登录")
    @PostMapping("/otp/login")
    public Result<Map<String, Object>> loginOtp(@RequestBody Map<String, Object> body) {
        String phone = str(body == null ? null : body.get("phone"));
        String code = str(body == null ? null : body.get("code"));
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        String cached = redisService.get("public:otp:" + phone);
        if (cached == null || !String.valueOf(cached).equals(code)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        Runner runner = runnerMapper.selectByPhone(phone);
        if (runner == null) {
            throw new RuntimeException("该手机号尚未报名，请先完成赛事报名");
        }
        redisService.delete("public:otp:" + phone);
        String token = UUID.randomUUID().toString().replace("-", "");
        redisService.set("public:runner:" + token, runner.getId(), 7, TimeUnit.DAYS);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("runnerToken", token);
        data.put("runnerId", runner.getId());
        data.put("name", runner.getName());
        data.put("phone", runner.getPhone());
        return Result.success(data);
    }

    /** 站内通知列表 */
    @ApiOperation("公开：站内通知")
    @GetMapping("/notices")
    public Result<List<Map<String, Object>>> notices(
            @RequestParam(required = false) String phone,
            @RequestHeader(value = "X-Runner-Token", required = false) String runnerToken) {
        Long runnerId = resolveRunnerId(phone, runnerToken);
        List<Map<String, Object>> list = noticeMapper.listByRunner(runnerId);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> n : list) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", n.get("id"));
            item.put("title", n.get("title"));
            item.put("content", n.get("content"));
            item.put("createdAt", first(n, "createdAt", "created_at"));
            item.put("readFlag", first(n, "readFlag", "read_flag"));
            out.add(item);
        }
        return Result.success(out);
    }

    /** 跑者自助退赛（已支付则生成可追踪退款单） */
    @ApiOperation("公开：自助退赛")
    @PostMapping("/cancel")
    public Result<Map<String, Object>> cancel(@RequestBody Map<String, Object> body,
                                  @RequestHeader(value = "X-Runner-Token", required = false) String runnerToken) {
        Long registrationId = toLong(body == null ? null : body.get("registrationId"));
        String phone = str(body == null ? null : body.get("phone"));
        if (registrationId == null) {
            throw new RuntimeException("缺少报名号");
        }
        Long runnerId = resolveRunnerId(phone, runnerToken);
        return Result.success(registrationService.runnerCancel(registrationId, runnerId));
    }

    private Long resolveRunnerId(String phone, String runnerToken) {
        if (runnerToken != null && !runnerToken.trim().isEmpty()) {
            Object id = redisService.get("public:runner:" + runnerToken.trim());
            if (id instanceof Number) {
                return ((Number) id).longValue();
            }
            if (id != null) {
                try {
                    return Long.valueOf(String.valueOf(id));
                } catch (Exception ignore) {
                    // fall through
                }
            }
        }
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("请先验证码登录，或提供有效手机号");
        }
        Runner runner = runnerMapper.selectByPhone(phone);
        if (runner == null) {
            throw new RuntimeException("未找到该手机号对应的选手");
        }
        return runner.getId();
    }

    /**
     * 3. 选手用手机号查自己的报名和成绩
     */
    @ApiOperation("公开：按手机号查我的报名与成绩")
    @GetMapping("/my")
    public Result<Map<String, Object>> my(
            @ApiParam(value = "手机号", required = true) @RequestParam String phone,
            @RequestHeader(value = "X-Runner-Token", required = false) String runnerToken) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("手机号不能为空");
        }
        Map<String, Object> runner = publicMapper.selectRunnerByPhone(phone.trim());
        if (runner == null) {
            throw new RuntimeException("未找到该手机号对应的选手，请先在前台报名");
        }

        Long runnerId = ((Number) runner.get("id")).longValue();
        boolean verified = false;
        if (runnerToken != null && !runnerToken.trim().isEmpty()) {
            try {
                Long tokenRunner = resolveRunnerId(null, runnerToken);
                verified = tokenRunner != null && tokenRunner.equals(runnerId);
            } catch (Exception ignore) {
                verified = false;
            }
        }

        List<Map<String, Object>> regs = publicMapper.selectMyRegistrations(runnerId);

        List<Map<String, Object>> ranked = publicMapper.selectResultsWithRanksByRunnerId(runnerId);
        Map<Long, Map<String, Object>> resultByRegId = new HashMap<Long, Map<String, Object>>();
        for (Map<String, Object> res : ranked) {
            Long rid = toLong(first(res, "registrationId", "registration_id"));
            if (rid != null) {
                resultByRegId.put(rid, res);
            }
        }

        List<Map<String, Object>> regList = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> r : regs) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            Long regId = toLong(first(r, "registrationId", "registration_id"));
            Object eventDate = first(r, "eventDate", "event_date");
            Object bib = first(r, "bibNumber", "bib_number");
            Map<String, Object> raw = regId == null ? null : resultByRegId.get(regId);
            boolean hasResult = raw != null;

            item.put("registrationId", regId);
            item.put("eventName", first(r, "eventName", "event_name"));
            item.put("eventDate", eventDate);
            item.put("bibNumber", bib);
            item.put("shirtSize", first(r, "shirtSize", "shirt_size"));
            item.put("orderNo", first(r, "orderNo", "order_no"));
            item.put("registerTime", first(r, "registerTime", "register_time"));
            item.put("payStatus", first(r, "payStatus", "pay_status"));
            item.put("payChannel", first(r, "payChannel", "pay_channel"));
            item.put("payTime", first(r, "payTime", "pay_time"));
            item.put("payVoucher", first(r, "payVoucher", "pay_voucher"));
            item.put("refundStatus", first(r, "refundStatus", "refund_status"));
            item.put("refundNo", first(r, "refundNo", "refund_no"));
            item.put("refundAmount", first(r, "refundAmount", "refund_amount"));
            item.put("refundTime", first(r, "refundTime", "refund_time"));
            item.put("entryFee", first(r, "entryFee", "entry_fee"));
            item.put("status", r.get("status"));
            item.put("distanceKm", first(r, "distanceKm", "distance_km"));
            item.put("cancelRule", first(r, "cancelRule", "cancel_rule"));
            item.put("bibHint", bibHint(eventDate == null ? null : String.valueOf(eventDate)));
            item.put("bibPending", bib == null || String.valueOf(bib).trim().isEmpty());
            item.put("resultEta", hasResult ? null : "预计比赛日结束后 2–24 小时内公布完整成绩");
            // 可退赛：有效报名 + 赛前 7 天窗口 + 尚无完赛成绩
            boolean canCancel = false;
            if (!hasResult && eventDate != null
                    && (r.get("status") == null || "1".equals(String.valueOf(r.get("status"))))) {
                try {
                    LocalDate ed = LocalDate.parse(String.valueOf(eventDate).substring(0, 10));
                    canCancel = LocalDate.now().isBefore(ed.minusDays(7));
                } catch (Exception ignore) {
                    canCancel = false;
                }
            }
            item.put("canCancel", canCancel);

            if (hasResult) {
                item.put("result", buildResultView(raw));
            } else {
                item.put("result", null);
            }
            regList.add(item);
        }

        Map<String, Object> runnerView = new LinkedHashMap<String, Object>();
        runnerView.put("id", runner.get("id"));
        runnerView.put("gender", runner.get("gender"));
        if (verified) {
            runnerView.put("name", runner.get("name"));
            runnerView.put("phone", runner.get("phone"));
        } else {
            runnerView.put("name", maskName(runner.get("name") == null ? null : String.valueOf(runner.get("name"))));
            runnerView.put("phone", maskPhone(phone.trim()));
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("runner", runnerView);
        data.put("registrations", regList);
        data.put("verified", verified);
        data.put("privacyHint", verified ? null : "未登录仅展示脱敏信息。验证码登录后可查看完整资料、支付与退赛。");
        return Result.success(data);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 4. 赛事公开排名 */
    @ApiOperation("公开：赛事成绩排名")
    @GetMapping("/rank/{eventId}")
    public Result<List<Map<String, Object>>> rank(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long eventId,
            @ApiParam(value = "号码布（模糊）") @RequestParam(required = false) String bibNumber,
            @ApiParam(value = "号码段前缀，如 A / A0 / B00") @RequestParam(required = false) String bibPrefix,
            @ApiParam(value = "省份代码（身份证前2位），如 61=陕西") @RequestParam(required = false) String province,
            @ApiParam(value = "姓名（模糊）") @RequestParam(required = false) String name,
            @ApiParam(value = "关键词（姓名或号码布）") @RequestParam(required = false) String keyword) {
        if (eventMapper.selectById(eventId) == null) {
            throw new RuntimeException("赛事不存在");
        }
        List<Map<String, Object>> list = publicMapper.selectPublicRank(
                eventId,
                blankToNull(bibNumber),
                blankToNull(bibPrefix),
                blankToNull(province),
                blankToNull(name),
                blankToNull(keyword));
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : list) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            Object overall = first(row, "overallRank", "overall_rank");
            if (overall == null) {
                overall = first(row, "rank", "RANK");
            }
            item.put("rank", overall);
            item.put("overallRank", overall);
            item.put("genderRank", first(row, "genderRank", "gender_rank"));
            item.put("ageRank", first(row, "ageRank", "age_rank"));
            item.put("ageGroup", first(row, "ageGroup", "age_group"));
            Object nm = first(row, "runnerName", "runner_name");
            if (nm == null) {
                nm = row.get("name");
            }
            item.put("name", nm);
            item.put("gender", row.get("gender"));
            item.put("bibNumber", first(row, "bibNumber", "bib_number"));
            Object prov = first(row, "provinceCode", "province_code");
            item.put("provinceCode", prov);
            item.put("province", provinceName(prov));
            item.put("chipTime", first(row, "chipTime", "chip_time"));
            item.put("chipTimeStr", first(row, "chipTimeStr", "chip_time_str"));
            item.put("netTime", first(row, "netTime", "net_time"));
            item.put("netTimeStr", first(row, "netTimeStr", "net_time_str"));
            Object dist = first(row, "distanceKm", "distance_km");
            item.put("distanceKm", dist);
            item.put("pace", paceStr(first(row, "netTime", "net_time"), dist));
            out.add(item);
        }
        return Result.success(out);
    }

    // ---------- 工具 ----------

    /** 身份证前 2 位 → 省份名（排行榜筛选展示） */
    private static String provinceName(Object codeObj) {
        if (codeObj == null) {
            return null;
        }
        String code = String.valueOf(codeObj).trim();
        if (code.length() >= 2) {
            code = code.substring(0, 2);
        }
        switch (code) {
            case "11": return "北京";
            case "12": return "天津";
            case "13": return "河北";
            case "14": return "山西";
            case "15": return "内蒙古";
            case "21": return "辽宁";
            case "22": return "吉林";
            case "23": return "黑龙江";
            case "31": return "上海";
            case "32": return "江苏";
            case "33": return "浙江";
            case "34": return "安徽";
            case "35": return "福建";
            case "36": return "江西";
            case "37": return "山东";
            case "41": return "河南";
            case "42": return "湖北";
            case "43": return "湖南";
            case "44": return "广东";
            case "45": return "广西";
            case "46": return "海南";
            case "50": return "重庆";
            case "51": return "四川";
            case "52": return "贵州";
            case "53": return "云南";
            case "54": return "西藏";
            case "61": return "陕西";
            case "62": return "甘肃";
            case "63": return "青海";
            case "64": return "宁夏";
            case "65": return "新疆";
            default: return code.isEmpty() ? null : code;
        }
    }

    private static String maskName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "*";
        }
        String n = name.trim();
        if (n.length() == 1) {
            return n;
        }
        if (n.length() == 2) {
            return n.charAt(0) + "*";
        }
        return n.charAt(0) + "*" + n.charAt(n.length() - 1);
    }

    /** 号码布发放预期说明（比赛日前 7–14 天） */
    private static String bibHint(String eventDate) {
        String window = "比赛日前 7–14 天";
        String from = null;
        if (eventDate != null && eventDate.length() >= 10) {
            try {
                LocalDate d = LocalDate.parse(eventDate.substring(0, 10));
                from = d.minusDays(14).toString();
                String to = d.minusDays(7).toString();
                window = from + " 至 " + to;
            } catch (Exception ignore) {
                // keep default window text
            }
        }
        return "号码布预计于 " + window + " 通过站内通知发放，发号后可在本页查看；领物时请携带确认函、证件与参赛服尺码信息。";
    }

    private static Map<String, Object> buildResultView(Map<String, Object> raw) {
        Map<String, Object> resultView = new LinkedHashMap<String, Object>();
        Object chip = first(raw, "chipTime", "chip_time");
        Object net = first(raw, "netTime", "net_time");
        Object dist = first(raw, "distanceKm", "distance_km");
        resultView.put("chipTime", chip);
        resultView.put("chipTimeStr", first(raw, "chipTimeStr", "chip_time_str"));
        resultView.put("netTime", net);
        resultView.put("netTimeStr", first(raw, "netTimeStr", "net_time_str"));
        resultView.put("split5k", first(raw, "split5k", "split_5k"));
        resultView.put("split5kStr", first(raw, "split5kStr", "split_5k_str"));
        resultView.put("split10k", first(raw, "split10k", "split_10k"));
        resultView.put("split10kStr", first(raw, "split10kStr", "split_10k_str"));
        resultView.put("split21k", first(raw, "split21k", "split_21k"));
        resultView.put("split21kStr", first(raw, "split21kStr", "split_21k_str"));
        resultView.put("finishStatus", first(raw, "finishStatus", "finish_status"));
        resultView.put("rank", first(raw, "rank", "rank"));
        resultView.put("genderRank", first(raw, "genderRank", "gender_rank"));
        resultView.put("ageRank", first(raw, "ageRank", "age_rank"));
        resultView.put("ageGroup", first(raw, "ageGroup", "age_group"));
        resultView.put("distanceKm", dist);
        resultView.put("pace", paceStr(net, dist));
        return resultView;
    }

    /** 配速：分'秒/公里 */
    private static String paceStr(Object netTimeObj, Object distanceKmObj) {
        if (netTimeObj == null || distanceKmObj == null) {
            return null;
        }
        double dist;
        int net;
        try {
            dist = new BigDecimal(String.valueOf(distanceKmObj)).doubleValue();
            net = Integer.parseInt(String.valueOf(netTimeObj));
        } catch (Exception e) {
            return null;
        }
        if (dist <= 0 || net <= 0) {
            return null;
        }
        double secPerKm = net / dist;
        int totalSec = (int) Math.round(secPerKm);
        int m = totalSec / 60;
        int s = totalSec % 60;
        return m + "'" + String.format("%02d", s) + "/km";
    }

    private static Object first(Map<String, Object> m, String camel, String snake) {
        if (m.containsKey(camel) && m.get(camel) != null) {
            return m.get(camel);
        }
        return m.get(snake);
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static int toInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
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

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

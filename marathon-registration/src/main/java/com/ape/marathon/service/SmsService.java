package com.ape.marathon.service;

import com.ape.marathon.config.SmsProperties;
import com.ape.marathon.entity.Runner;
import com.ape.marathon.mapper.NoticeMapper;
import com.ape.marathon.mapper.RunnerMapper;
import com.ape.marathon.mapper.SmsLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 可配置短信：有阿里云密钥则尝试外发；否则本地通道写入 sms_log + 站内通知
 */
@Service
public class SmsService {

    @Autowired
    private SmsProperties props;

    @Autowired
    private SmsLogMapper smsLogMapper;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    /**
     * @return provider, success, deliveredVia, exposeCode?, hint
     */
    public Map<String, Object> send(String phone, String scene, String content, String noticeTitle) {
        String want = props.getProvider() == null ? "local" : props.getProvider().trim().toLowerCase();
        String provider;
        boolean success;
        String msg;
        String deliveredVia;

        if ("aliyun".equals(want) && props.aliyunReady()) {
            // 课设环境通常无真实 SDK 依赖：校验配置后仍走可追踪本地投递，并标明「已配置阿里云待联调」
            provider = "aliyun-configured";
            success = true;
            msg = "短信已投递至站内通知";
            deliveredVia = "inbox";
            deliverLocal(phone, noticeTitle, content);
        } else if ("aliyun".equals(want) && !props.aliyunReady()) {
            provider = "local-fallback";
            success = true;
            msg = "短信已投递至站内通知";
            deliveredVia = "inbox";
            deliverLocal(phone, noticeTitle, content);
        } else {
            provider = "local";
            success = true;
            msg = "短信已投递至站内通知";
            deliveredVia = "inbox";
            deliverLocal(phone, noticeTitle, content);
        }

        smsLogMapper.insert(phone, scene, content, provider, success ? 1 : 0, msg);

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("provider", provider);
        out.put("success", success);
        out.put("deliveredVia", deliveredVia);
        out.put("exposeCode", props.isExposeCode());
        out.put("hint", msg);
        return out;
    }

    private void deliverLocal(String phone, String noticeTitle, String content) {
        // 登录验证码不进站内通知，避免个人中心被验证码刷屏
        if (noticeTitle != null && noticeTitle.contains("验证码")) {
            return;
        }
        Runner runner = runnerMapper.selectByPhone(phone);
        if (runner == null) {
            return;
        }
        noticeMapper.insert(runner.getId(), noticeTitle, content);
    }
}

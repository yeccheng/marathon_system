package com.ape.marathon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信通道：local=站内信+短信日志；aliyun=有密钥则调阿里云，否则降级 local
 */
@Component
@ConfigurationProperties(prefix = "marathon.sms")
public class SmsProperties {

    /** local | aliyun */
    private String provider = "local";
    /** 本地/降级时是否在接口回显验证码（仅联调；生产建议 false，走站内通知） */
    private boolean exposeCode = true;
    private String signName = "发令台";
    private String templateCode = "";
    private String accessKeyId = "";
    private String accessKeySecret = "";

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public boolean isExposeCode() { return exposeCode; }
    public void setExposeCode(boolean exposeCode) { this.exposeCode = exposeCode; }
    public String getSignName() { return signName; }
    public void setSignName(String signName) { this.signName = signName; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
    public String getAccessKeySecret() { return accessKeySecret; }
    public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }

    public boolean aliyunReady() {
        return accessKeyId != null && !accessKeyId.trim().isEmpty()
                && accessKeySecret != null && !accessKeySecret.trim().isEmpty()
                && templateCode != null && !templateCode.trim().isEmpty();
    }
}

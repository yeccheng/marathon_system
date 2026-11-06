package com.ape.marathon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付通道：cashier=收银台闭环写 payment_log；
 * gateway=mock 即时到账；配置商户号后可切换 wechat/alipay 预留字段（无密钥仍走 mock 并注明）
 */
@Component
@ConfigurationProperties(prefix = "marathon.pay")
public class PayProperties {

    /** mock | wechat | alipay */
    private String gateway = "mock";
    private String merchantName = "发令台赛事组委会";
    private String wechatMchId = "";
    private String alipayAppId = "";

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getWechatMchId() { return wechatMchId; }
    public void setWechatMchId(String wechatMchId) { this.wechatMchId = wechatMchId; }
    public String getAlipayAppId() { return alipayAppId; }
    public void setAlipayAppId(String alipayAppId) { this.alipayAppId = alipayAppId; }
}

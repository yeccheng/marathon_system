package com.ape.marathon.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报名记录实体类（对应表 registration）
 */
@ApiModel(value = "Registration", description = "报名记录")
public class Registration {

    /** 报名记录ID */
    @ApiModelProperty(value = "报名记录ID", example = "1")
    private Long id;

    /** 赛事ID */
    @ApiModelProperty(value = "赛事ID", example = "1", required = true)
    private Long eventId;

    /** 选手ID */
    @ApiModelProperty(value = "选手ID", example = "1", required = true)
    private Long runnerId;

    /** 号码布编号 */
    @ApiModelProperty(value = "号码布编号", example = "A1001")
    private String bibNumber;

    /** 参赛服尺码 XS/S/M/L/XL/XXL */
    @ApiModelProperty(value = "参赛服尺码", example = "L")
    private String shirtSize;

    /** 订单号 */
    @ApiModelProperty(value = "订单号", example = "GS202611150001")
    private String orderNo;

    /** 报名时间 */
    @ApiModelProperty(value = "报名时间", example = "2026-01-01 12:00:00", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime registerTime;

    /** 支付状态：0未支付，1已支付，2已退款 */
    @ApiModelProperty(value = "支付状态：0未支付，1已支付，2已退款", example = "0")
    private Integer payStatus;

    /** 支付渠道 wechat/alipay/mock */
    private String payChannel;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime payTime;

    /** 支付凭证号 */
    private String payVoucher;

    /** 退款状态：0无，1成功，2处理中 */
    private Integer refundStatus;

    private String refundNo;
    private BigDecimal refundAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime refundTime;

    /** 报名状态：1已报名，2已取消，3退赛 */
    @ApiModelProperty(value = "报名状态：1已报名，2已取消，3退赛", example = "1")
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public Long getRunnerId() { return runnerId; }
    public void setRunnerId(Long runnerId) { this.runnerId = runnerId; }
    public String getBibNumber() { return bibNumber; }
    public void setBibNumber(String bibNumber) { this.bibNumber = bibNumber; }
    public String getShirtSize() { return shirtSize; }
    public void setShirtSize(String shirtSize) { this.shirtSize = shirtSize; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public LocalDateTime getRegisterTime() { return registerTime; }
    public void setRegisterTime(LocalDateTime registerTime) { this.registerTime = registerTime; }
    public Integer getPayStatus() { return payStatus; }
    public void setPayStatus(Integer payStatus) { this.payStatus = payStatus; }
    public String getPayChannel() { return payChannel; }
    public void setPayChannel(String payChannel) { this.payChannel = payChannel; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
    public String getPayVoucher() { return payVoucher; }
    public void setPayVoucher(String payVoucher) { this.payVoucher = payVoucher; }
    public Integer getRefundStatus() { return refundStatus; }
    public void setRefundStatus(Integer refundStatus) { this.refundStatus = refundStatus; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public LocalDateTime getRefundTime() { return refundTime; }
    public void setRefundTime(LocalDateTime refundTime) { this.refundTime = refundTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}

package com.ape.marathon.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 跨服务赛事摘要（报名/成绩 Feign 使用）
 */
public class EventBrief {

    private Long id;
    private String eventName;
    private Integer eventType;
    private LocalDate eventDate;
    private String startLocation;
    private BigDecimal distanceKm;
    private Integer quota;
    private Integer registeredCount;
    private Integer remainQuota;
    private BigDecimal entryFee;
    private Integer status;
    private LocalDateTime signupStart;
    private LocalDateTime signupEnd;
    private String cancelRule;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public Integer getEventType() { return eventType; }
    public void setEventType(Integer eventType) { this.eventType = eventType; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }
    public Integer getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(Integer registeredCount) { this.registeredCount = registeredCount; }
    public Integer getRemainQuota() { return remainQuota; }
    public void setRemainQuota(Integer remainQuota) { this.remainQuota = remainQuota; }
    public BigDecimal getEntryFee() { return entryFee; }
    public void setEntryFee(BigDecimal entryFee) { this.entryFee = entryFee; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getSignupStart() { return signupStart; }
    public void setSignupStart(LocalDateTime signupStart) { this.signupStart = signupStart; }
    public LocalDateTime getSignupEnd() { return signupEnd; }
    public void setSignupEnd(LocalDateTime signupEnd) { this.signupEnd = signupEnd; }
    public String getCancelRule() { return cancelRule; }
    public void setCancelRule(String cancelRule) { this.cancelRule = cancelRule; }
}

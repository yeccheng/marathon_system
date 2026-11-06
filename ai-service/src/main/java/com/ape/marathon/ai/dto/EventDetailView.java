package com.ape.marathon.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 赛事详情视图（公开详情接口精简字段，避免把超长 JSON 塞进模型上下文）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDetailView {

    private Long id;
    private String eventName;
    private Integer eventType;
    private String eventDate;
    private String startLocation;
    private BigDecimal distanceKm;
    private Integer quota;
    private Integer registeredCount;
    private Integer remainQuota;
    private BigDecimal entryFee;
    private Integer status;
    private String signupStart;
    private String signupEnd;
    private String gunTime;
    private String cutOffTime;
    private String signupRule;
    private String pickupInfo;
    private String routeDesc;
    private String cancelRule;
    private Integer finisherCount;
    private Boolean hasResults;
    private String phaseLabel;
    private String resultHint;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Integer getEventType() {
        return eventType;
    }

    public void setEventType(Integer eventType) {
        this.eventType = eventType;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public Integer getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(Integer registeredCount) {
        this.registeredCount = registeredCount;
    }

    public Integer getRemainQuota() {
        return remainQuota;
    }

    public void setRemainQuota(Integer remainQuota) {
        this.remainQuota = remainQuota;
    }

    public BigDecimal getEntryFee() {
        return entryFee;
    }

    public void setEntryFee(BigDecimal entryFee) {
        this.entryFee = entryFee;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getSignupStart() {
        return signupStart;
    }

    public void setSignupStart(String signupStart) {
        this.signupStart = signupStart;
    }

    public String getSignupEnd() {
        return signupEnd;
    }

    public void setSignupEnd(String signupEnd) {
        this.signupEnd = signupEnd;
    }

    public String getGunTime() {
        return gunTime;
    }

    public void setGunTime(String gunTime) {
        this.gunTime = gunTime;
    }

    public String getCutOffTime() {
        return cutOffTime;
    }

    public void setCutOffTime(String cutOffTime) {
        this.cutOffTime = cutOffTime;
    }

    public String getSignupRule() {
        return signupRule;
    }

    public void setSignupRule(String signupRule) {
        this.signupRule = signupRule;
    }

    public String getPickupInfo() {
        return pickupInfo;
    }

    public void setPickupInfo(String pickupInfo) {
        this.pickupInfo = pickupInfo;
    }

    public String getRouteDesc() {
        return routeDesc;
    }

    public void setRouteDesc(String routeDesc) {
        this.routeDesc = routeDesc;
    }

    public String getCancelRule() {
        return cancelRule;
    }

    public void setCancelRule(String cancelRule) {
        this.cancelRule = cancelRule;
    }

    public Integer getFinisherCount() {
        return finisherCount;
    }

    public void setFinisherCount(Integer finisherCount) {
        this.finisherCount = finisherCount;
    }

    public Boolean getHasResults() {
        return hasResults;
    }

    public void setHasResults(Boolean hasResults) {
        this.hasResults = hasResults;
    }

    public String getPhaseLabel() {
        return phaseLabel;
    }

    public void setPhaseLabel(String phaseLabel) {
        this.phaseLabel = phaseLabel;
    }

    public String getResultHint() {
        return resultHint;
    }

    public void setResultHint(String resultHint) {
        this.resultHint = resultHint;
    }
}

package com.ape.marathon.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 赛事实体类（对应表 marathon_event）
 */
public class MarathonEvent {

    private Long id;
    private String eventName;
    private Integer eventType;

@JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate eventDate;

    private String startLocation;
    private BigDecimal distanceKm;
    private Integer quota;
    private Integer registeredCount;
    private BigDecimal entryFee;
    private Integer status;

    /** 报名开始时间 */
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime signupStart;

    /** 报名截止时间 */
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime signupEnd;

    /** 枪声发令时间 */
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime gunTime;

    /** 关门时间描述 */
    private String cutOffTime;

    /** 报名规则 */
    private String signupRule;

    /** 领物信息 */
    private String pickupInfo;

    /** 赛道简介 */
    private String routeDesc;

    /** 补给站 JSON：[{km,name,items}] */
    private String aidStations;

    /** 海拔剖面 JSON：[{km,elev}] */
    private String routeProfile;

    /** GPX 路线文件路径，如 /routes/event-1.gpx */
    private String routeGpx;

    /** 跑者手册 JSON：handbook/medical/insurance/transport/hotel/contact */
    private String guideJson;

    /** 退赛规则说明 */
    private String cancelRule;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 剩余名额（查询时计算） */
    private Integer remainQuota;

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
    public BigDecimal getEntryFee() { return entryFee; }
    public void setEntryFee(BigDecimal entryFee) { this.entryFee = entryFee; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getSignupStart() { return signupStart; }
    public void setSignupStart(LocalDateTime signupStart) { this.signupStart = signupStart; }
    public LocalDateTime getSignupEnd() { return signupEnd; }
    public void setSignupEnd(LocalDateTime signupEnd) { this.signupEnd = signupEnd; }
    public LocalDateTime getGunTime() { return gunTime; }
    public void setGunTime(LocalDateTime gunTime) { this.gunTime = gunTime; }
    public String getCutOffTime() { return cutOffTime; }
    public void setCutOffTime(String cutOffTime) { this.cutOffTime = cutOffTime; }
    public String getSignupRule() { return signupRule; }
    public void setSignupRule(String signupRule) { this.signupRule = signupRule; }
    public String getPickupInfo() { return pickupInfo; }
    public void setPickupInfo(String pickupInfo) { this.pickupInfo = pickupInfo; }
    public String getRouteDesc() { return routeDesc; }
    public void setRouteDesc(String routeDesc) { this.routeDesc = routeDesc; }
    public String getAidStations() { return aidStations; }
    public void setAidStations(String aidStations) { this.aidStations = aidStations; }
    public String getRouteProfile() { return routeProfile; }
    public void setRouteProfile(String routeProfile) { this.routeProfile = routeProfile; }
    public String getRouteGpx() { return routeGpx; }
    public void setRouteGpx(String routeGpx) { this.routeGpx = routeGpx; }
    public String getGuideJson() { return guideJson; }
    public void setGuideJson(String guideJson) { this.guideJson = guideJson; }
    public String getCancelRule() { return cancelRule; }
    public void setCancelRule(String cancelRule) { this.cancelRule = cancelRule; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getRemainQuota() { return remainQuota; }
    public void setRemainQuota(Integer remainQuota) { this.remainQuota = remainQuota; }
}
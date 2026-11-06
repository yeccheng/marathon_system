package com.ape.marathon.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;

/**
 * 比赛成绩实体类（对应表 result）
 * 一条报名记录只能有一条成绩（registration_id 唯一）
 */
@ApiModel(value = "Result", description = "比赛成绩")
public class Result {

    /** 成绩ID */
    @ApiModelProperty(value = "成绩ID", example = "1")
    private Long id;

    /** 报名记录ID */
    @ApiModelProperty(value = "报名记录ID", example = "1", required = true)
    @NotNull(message = "报名记录ID不能为空")
    private Long registrationId;

    /** 枪声成绩（秒） */
    @ApiModelProperty(value = "枪声成绩（秒）", example = "12600")
    @Positive(message = "枪声成绩必须大于 0")
    private Integer chipTime;

    /** 净成绩（秒） */
    @ApiModelProperty(value = "净成绩（秒）", example = "12540")
    private Integer netTime;

    /**
     * 完赛状态：按需求校验 1~3
     * （业务上常见为 1 完赛 / 0 未完赛；若前端仍传 0，会触发校验失败）
     */
    @ApiModelProperty(value = "完赛状态：1~3", example = "1")
    @NotNull(message = "完赛状态不能为空")
    @Min(value = 1, message = "完赛状态只能是 1~3")
    @Max(value = 3, message = "完赛状态只能是 1~3")
    private Integer finishStatus;

    /** 完赛时间 */
    @ApiModelProperty(value = "完赛时间", example = "2027-04-18 11:30:00")
    @NotNull(message = "完赛时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime finishTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Integer getChipTime() { return chipTime; }
    public void setChipTime(Integer chipTime) { this.chipTime = chipTime; }
    public Integer getNetTime() { return netTime; }
    public void setNetTime(Integer netTime) { this.netTime = netTime; }
    public Integer getFinishStatus() { return finishStatus; }
    public void setFinishStatus(Integer finishStatus) { this.finishStatus = finishStatus; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }
}

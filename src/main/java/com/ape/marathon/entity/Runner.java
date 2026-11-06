package com.ape.marathon.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 选手档案实体类（对应表 runner）
 */
@ApiModel(value = "Runner", description = "选手档案")
public class Runner {

    /** 选手ID */
    @ApiModelProperty(value = "选手ID", example = "1")
    private Long id;

    /** 姓名 */
    @ApiModelProperty(value = "姓名", example = "张三", required = true)
    @NotBlank(message = "选手姓名不能为空")
    private String name;

    /** 性别：1男，2女 */
    @ApiModelProperty(value = "性别：1男，2女", example = "1", required = true)
    @NotNull(message = "性别不能为空")
    @Min(value = 1, message = "性别只能是 1（男）或 2（女）")
    @Max(value = 2, message = "性别只能是 1（男）或 2（女）")
    private Integer gender;

    /** 身份证号（唯一） */
    @ApiModelProperty(value = "身份证号（唯一）", example = "610102199001011234")
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{16}[\\dXx]$", message = "身份证号须为 18 位（末位可为数字或 X）")
    private String idCard;

    /** 手机号 */
    @ApiModelProperty(value = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确（须为 1 开头的 11 位）")
    private String phone;

    /** 出生日期 */
    @ApiModelProperty(value = "出生日期", example = "1990-01-01")
    @NotNull(message = "出生日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    /** 紧急联系人及电话 */
    @ApiModelProperty(value = "紧急联系人及电话", example = "李四 13900139000")
    private String emergencyContact;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间", example = "2026-01-01 12:00:00", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

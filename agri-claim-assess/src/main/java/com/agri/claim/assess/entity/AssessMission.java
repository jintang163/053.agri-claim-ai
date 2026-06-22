package com.agri.claim.assess.entity;

import com.agri.claim.common.core.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assess_mission")
public class AssessMission extends BaseEntity {

    private String missionNo;
    private String missionName;
    private Long policyId;
    private String policyNo;
    private String policyHolderName;
    private String idCardNo;
    private String phone;
    private String address;
    private String cropType;
    private BigDecimal insuredArea;
    private BigDecimal insuredAmount;
    private String disasterType;
    private String disasterLevel;
    private String disasterDate;
    private String disasterLocation;
    private BigDecimal disasterCenterLon;
    private BigDecimal disasterCenterLat;
    private Long beforeImageId;
    private Long afterImageId;
    private BigDecimal disasterArea;
    private BigDecimal disasterRatio;
    private BigDecimal estimateAmount;
    private BigDecimal finalAmount;
    private String assessStatus;
    private String surveyorName;
    private String surveyorPhone;
    private String auditorName;
    private LocalDateTime auditTime;
    private String auditRemark;
    private String reportPath;
    private String reportNo;
    private LocalDateTime reportTime;
    private String remark;

    @TableField(exist = false)
    private List<AssessDetail> details;
}

package com.agri.claim.assess.entity;

import com.agri.claim.common.core.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assess_detail")
public class AssessDetail extends BaseEntity {

    private Long missionId;
    private String missionNo;
    private Long segmentId;
    private String cropType;
    private String disasterType;
    private String disasterLevel;
    private BigDecimal plotArea;
    private BigDecimal disasterArea;
    private BigDecimal disasterRatio;
    private BigDecimal unitYield;
    private BigDecimal unitPrice;
    private BigDecimal disasterCoeff;
    private BigDecimal compensateRatio;
    private BigDecimal detailAmount;
    private BigDecimal adjustCoeff;
    private BigDecimal adjustAmount;
    private BigDecimal finalAmount;
    private String polygonWkt;
    private String remark;
}

package com.agri.claim.assess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AssessMissionDTO {

    @NotBlank(message = "任务名称不能为空")
    private String missionName;

    private String policyNo;
    private String policyHolderName;
    private String idCardNo;
    private String phone;
    private String address;

    @NotBlank(message = "作物类型不能为空")
    private String cropType;

    private BigDecimal insuredArea;
    private BigDecimal insuredAmount;

    @NotBlank(message = "灾害类型不能为空")
    private String disasterType;

    private String disasterDate;
    private String disasterLocation;
    private BigDecimal disasterCenterLon;
    private BigDecimal disasterCenterLat;

    @NotNull(message = "灾前影像不能为空")
    private Long beforeImageId;

    @NotNull(message = "灾后影像不能为空")
    private Long afterImageId;

    private BigDecimal adjustCoeff;
    private String remark;
    private List<Long> adjustDetailIds;
}

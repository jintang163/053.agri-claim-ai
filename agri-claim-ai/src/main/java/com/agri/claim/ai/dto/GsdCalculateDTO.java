package com.agri.claim.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GsdCalculateDTO {

    @NotNull(message = "飞行高度不能为空")
    private BigDecimal flightHeight;

    private BigDecimal focalLength;

    private BigDecimal sensorWidth;

    private BigDecimal imageWidth;
}

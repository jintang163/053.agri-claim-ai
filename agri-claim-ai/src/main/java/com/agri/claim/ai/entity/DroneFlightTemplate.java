package com.agri.claim.ai.entity;

import com.agri.claim.common.core.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("drone_flight_template")
public class DroneFlightTemplate extends BaseEntity {

    private String templateName;

    private String templateDesc;

    private String locationName;

    private BigDecimal centerLon;

    private BigDecimal centerLat;

    private String polygonWkt;

    private BigDecimal flightHeight;

    private BigDecimal frontOverlap;

    private BigDecimal sideOverlap;

    private BigDecimal flightSpeed;

    private String cameraParamJson;

    private BigDecimal estimatedTime;

    private BigDecimal estimatedDistance;

    private BigDecimal estimatedArea;

    private Integer waypointCount;

    private Integer photoCount;

    private Integer estimatedBattery;

    private String routePlanJson;

    @TableLogic
    @JsonIgnore
    @TableField(value = "is_deleted")
    private Integer deleted;
}

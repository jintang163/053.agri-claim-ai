package com.agri.claim.ai.entity;

import com.agri.claim.common.core.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("drone_flight_task")
public class DroneFlightTask extends BaseEntity {

    private String taskNo;

    private String taskName;

    private Long templateId;

    private Long missionId;

    private String missionNo;

    private String aircraftSn;

    private String aircraftModel;

    private String payloadModel;

    private Long pilotId;

    private String pilotName;

    private BigDecimal centerLon;

    private BigDecimal centerLat;

    private String polygonWkt;

    private BigDecimal flightHeight;

    private BigDecimal frontOverlap;

    private BigDecimal sideOverlap;

    private BigDecimal flightSpeed;

    private BigDecimal takeoffLon;

    private BigDecimal takeoffLat;

    private String flightStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal actualDuration;

    private BigDecimal actualDistance;

    private Integer actualPhotoCount;

    private Integer batteryStart;

    private Integer batteryEnd;

    private String routePlanJson;

    private String resultJson;

    private String remark;

    @TableLogic
    @JsonIgnore
    @TableField(value = "is_deleted")
    private Integer deleted;
}

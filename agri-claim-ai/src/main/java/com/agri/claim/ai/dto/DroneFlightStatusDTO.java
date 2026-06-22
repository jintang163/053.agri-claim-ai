package com.agri.claim.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DroneFlightStatusDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "时间戳不能为空")
    private Long timestamp;

    private BigDecimal aircraftLon;

    private BigDecimal aircraftLat;

    private BigDecimal aircraftAltitude;

    private BigDecimal absoluteAltitude;

    private BigDecimal speedX;

    private BigDecimal speedY;

    private BigDecimal speedZ;

    private BigDecimal groundSpeed;

    private BigDecimal heading;

    private BigDecimal pitch;

    private BigDecimal roll;

    private BigDecimal yaw;

    private BigDecimal gimbalPitch;

    private BigDecimal gimbalYaw;

    private Integer batteryPercent;

    private BigDecimal batteryVoltage;

    private BigDecimal batteryCurrent;

    private BigDecimal batteryTemperature;

    private String flightMode;

    private Integer currentWaypointIndex;

    private Integer totalWaypoints;

    private BigDecimal distanceToHome;

    private Integer isFlying;

    private Integer isReturningHome;

    private Integer isLanding;

    private Integer isTakingOff;

    private String warnings;

    private String errors;

    private String rawJson;
}

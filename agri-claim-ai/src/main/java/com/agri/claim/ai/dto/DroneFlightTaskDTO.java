package com.agri.claim.ai.dto;

import com.agri.claim.ai.service.FlightRouteService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DroneFlightTaskDTO {

    private Long id;

    @NotBlank(message = "任务名称不能为空")
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

    private List<FlightRouteService.LatLng> polygonVertices;

    private BigDecimal flightHeight;

    private BigDecimal frontOverlap;

    private BigDecimal sideOverlap;

    private BigDecimal flightSpeed;

    private BigDecimal takeoffLon;

    private BigDecimal takeoffLat;

    private String remark;

    private List<FlightRouteService.Obstacle> obstacles;
}

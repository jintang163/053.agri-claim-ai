package com.agri.claim.ai.dto;

import com.agri.claim.ai.service.FlightRouteService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DroneFlightTemplateDTO {

    private Long id;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    private String templateDesc;

    private String locationName;

    private BigDecimal centerLon;

    private BigDecimal centerLat;

    private String polygonWkt;

    private List<FlightRouteService.LatLng> polygonVertices;

    private BigDecimal flightHeight;

    private BigDecimal frontOverlap;

    private BigDecimal sideOverlap;

    private BigDecimal flightSpeed;

    private String cameraParamJson;

    private List<FlightRouteService.Obstacle> obstacles;
}

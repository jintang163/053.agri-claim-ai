package com.agri.claim.ai.dto;

import com.agri.claim.ai.service.FlightRouteService;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoutePlanRequestDTO {

    @NotEmpty(message = "多边形顶点不能为空")
    private List<FlightRouteService.LatLng> polygonVertices;

    private BigDecimal flightHeight;

    private BigDecimal frontOverlap;

    private BigDecimal sideOverlap;

    private BigDecimal flightSpeed;

    private List<FlightRouteService.Obstacle> obstacles;

    private FlightRouteService.CameraParam cameraParam;
}

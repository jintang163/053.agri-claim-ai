package com.agri.claim.ai.dto;

import com.agri.claim.ai.service.FlightRouteService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ObstacleAvoidDTO {

    @NotEmpty(message = "航点列表不能为空")
    private List<FlightRouteService.Waypoint> waypoints;

    @NotEmpty(message = "障碍物列表不能为空")
    private List<FlightRouteService.Obstacle> obstacles;

    @NotNull(message = "默认飞行高度不能为空")
    private BigDecimal defaultHeight;

    private List<FlightRouteService.LatLng> polygonVertices;
}

package com.agri.claim.ai.controller;

import com.agri.claim.ai.dto.GsdCalculateDTO;
import com.agri.claim.ai.dto.ObstacleAvoidDTO;
import com.agri.claim.ai.dto.RoutePlanRequestDTO;
import com.agri.claim.ai.service.FlightRouteService;
import com.agri.claim.common.result.R;
import com.alibaba.fastjson2.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "航线规划")
@RestController
@RequestMapping("/ai/route")
@RequiredArgsConstructor
public class FlightRouteController {

    private final FlightRouteService flightRouteService;

    @Operation(summary = "生成之字形航线计划")
    @PostMapping("/plan")
    public R<FlightRouteService.RoutePlan> generateRoute(@Valid @RequestBody RoutePlanRequestDTO dto) {
        log.info("生成航线计划 | 顶点数: {} | 飞行高度: {}m",
                dto.getPolygonVertices().size(), dto.getFlightHeight());

        FlightRouteService.RoutePlanRequest request = new FlightRouteService.RoutePlanRequest();
        request.setPolygonVertices(dto.getPolygonVertices());
        request.setFlightHeight(dto.getFlightHeight() != null ? dto.getFlightHeight().doubleValue() : 100.0);
        request.setFrontOverlap(dto.getFrontOverlap() != null ? dto.getFrontOverlap().doubleValue() : 80.0);
        request.setSideOverlap(dto.getSideOverlap() != null ? dto.getSideOverlap().doubleValue() : 60.0);
        request.setFlightSpeed(dto.getFlightSpeed() != null ? dto.getFlightSpeed().doubleValue() : 5.0);
        request.setObstacles(dto.getObstacles());

        FlightRouteService.RoutePlan plan = flightRouteService.generateZigzagRoute(request);
        return R.ok("航线生成成功", plan);
    }

    @Operation(summary = "计算地面采样距离(GSD)")
    @PostMapping("/calculate-gsd")
    public R<Map<String, Object>> calculateGsd(@Valid @RequestBody GsdCalculateDTO dto) {
        log.info("计算GSD | 飞行高度: {}m", dto.getFlightHeight());

        double[] result = flightRouteService.calculateGroundSamplingDistance(dto.getFlightHeight().doubleValue());

        Map<String, Object> gsdResult = new HashMap<>();
        gsdResult.put("gsd", BigDecimal.valueOf(result[0]).setScale(3, RoundingMode.HALF_UP));
        gsdResult.put("groundCoverWidth", BigDecimal.valueOf(result[1]).setScale(3, RoundingMode.HALF_UP));
        gsdResult.put("groundCoverHeight", BigDecimal.valueOf(result[2]).setScale(3, RoundingMode.HALF_UP));
        gsdResult.put("flightHeight", dto.getFlightHeight());
        gsdResult.put("unit", "cm/pixel");

        return R.ok("GSD计算完成", gsdResult);
    }

    @Operation(summary = "应用障碍物规避算法")
    @PostMapping("/avoid-obstacles")
    public R<List<FlightRouteService.Waypoint>> avoidObstacles(@Valid @RequestBody ObstacleAvoidDTO dto) {
        log.info("应用障碍物规避 | 航点数: {} | 障碍物数: {}",
                dto.getWaypoints().size(), dto.getObstacles().size());

        List<FlightRouteService.Waypoint> adjustedWaypoints = flightRouteService.applyObstacleAvoidance(
                dto.getWaypoints(),
                dto.getObstacles(),
                dto.getDefaultHeight().doubleValue(),
                dto.getPolygonVertices()
        );

        log.info("障碍物规避完成 | 调整后航点数: {}", adjustedWaypoints.size());
        return R.ok("障碍物规避完成", adjustedWaypoints);
    }

    @Operation(summary = "预览航线JSON")
    @PostMapping("/preview")
    public R<String> previewRoute(@Valid @RequestBody RoutePlanRequestDTO dto) {
        FlightRouteService.RoutePlanRequest request = new FlightRouteService.RoutePlanRequest();
        request.setPolygonVertices(dto.getPolygonVertices());
        request.setFlightHeight(dto.getFlightHeight() != null ? dto.getFlightHeight().doubleValue() : 100.0);
        request.setFrontOverlap(dto.getFrontOverlap() != null ? dto.getFrontOverlap().doubleValue() : 80.0);
        request.setSideOverlap(dto.getSideOverlap() != null ? dto.getSideOverlap().doubleValue() : 60.0);
        request.setFlightSpeed(dto.getFlightSpeed() != null ? dto.getFlightSpeed().doubleValue() : 5.0);
        request.setObstacles(dto.getObstacles());

        FlightRouteService.RoutePlan plan = flightRouteService.generateZigzagRoute(request);
        return R.ok("航线预览", JSON.toJSONString(plan));
    }
}

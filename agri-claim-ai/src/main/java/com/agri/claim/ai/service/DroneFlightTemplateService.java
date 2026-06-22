package com.agri.claim.ai.service;

import com.agri.claim.ai.dto.DroneFlightTemplateDTO;
import com.agri.claim.ai.entity.DroneFlightTemplate;
import com.agri.claim.ai.mapper.DroneFlightTemplateMapper;
import com.agri.claim.ai.util.WktUtil;
import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.core.page.PageQuery;
import com.agri.claim.common.utils.SecurityUtils;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DroneFlightTemplateService extends ServiceImpl<DroneFlightTemplateMapper, DroneFlightTemplate>
        implements IService<DroneFlightTemplate> {

    private final FlightRouteService flightRouteService;

    public DroneFlightTemplate saveTemplate(DroneFlightTemplateDTO dto) {
        log.info("保存飞行模板 | 模板名称: {} | 创建人: {}", dto.getTemplateName(), SecurityUtils.getUserName());

        List<FlightRouteService.LatLng> vertices = dto.getPolygonVertices();
        if (vertices == null && dto.getPolygonWkt() != null) {
            vertices = WktUtil.parsePolygon(dto.getPolygonWkt());
        }

        FlightRouteService.RoutePlanRequest request = new FlightRouteService.RoutePlanRequest();
        request.setPolygonVertices(vertices);
        request.setFlightHeight(dto.getFlightHeight() != null ? dto.getFlightHeight().doubleValue() : 100.0);
        request.setFrontOverlap(dto.getFrontOverlap() != null ? dto.getFrontOverlap().doubleValue() : 80.0);
        request.setSideOverlap(dto.getSideOverlap() != null ? dto.getSideOverlap().doubleValue() : 60.0);
        request.setFlightSpeed(dto.getFlightSpeed() != null ? dto.getFlightSpeed().doubleValue() : 5.0);
        request.setObstacles(dto.getObstacles());
        request.setUserId(SecurityUtils.getUserId());

        FlightRouteService.RoutePlan plan = flightRouteService.generateZigzagRoute(request);

        DroneFlightTemplate template = new DroneFlightTemplate();
        template.setId(dto.getId());
        template.setTemplateName(dto.getTemplateName());
        template.setTemplateDesc(dto.getTemplateDesc());
        template.setLocationName(dto.getLocationName());

        if (vertices != null && !vertices.isEmpty()) {
            template.setCenterLon(dto.getCenterLon() != null ? dto.getCenterLon() : WktUtil.calculateCenterLon(vertices));
            template.setCenterLat(dto.getCenterLat() != null ? dto.getCenterLat() : WktUtil.calculateCenterLat(vertices));
            template.setPolygonWkt(dto.getPolygonWkt() != null ? dto.getPolygonWkt() : WktUtil.toWktPolygon(vertices));
        } else {
            template.setCenterLon(dto.getCenterLon());
            template.setCenterLat(dto.getCenterLat());
            template.setPolygonWkt(dto.getPolygonWkt());
        }

        template.setFlightHeight(plan.getFlightHeight());
        template.setFrontOverlap(plan.getFrontOverlap());
        template.setSideOverlap(plan.getSideOverlap());
        template.setFlightSpeed(dto.getFlightSpeed() != null ? dto.getFlightSpeed() : BigDecimal.valueOf(5.0));
        template.setCameraParamJson(dto.getCameraParamJson());
        template.setEstimatedTime(plan.getEstimatedTime());
        template.setEstimatedDistance(plan.getEstimatedDistance());
        template.setEstimatedArea(plan.getEstimatedArea());
        template.setWaypointCount(plan.getWaypointCount());
        template.setPhotoCount(plan.getPhotoCount());
        template.setEstimatedBattery(plan.getEstimatedBattery().intValue());
        template.setRoutePlanJson(JSON.toJSONString(plan));

        if (dto.getId() == null) {
            template.setCreateBy(SecurityUtils.getUserName());
            this.save(template);
            log.info("飞行模板创建成功 | 模板ID: {} | 航点数: {}", template.getId(), template.getWaypointCount());
        } else {
            this.updateById(template);
            log.info("飞行模板更新成功 | 模板ID: {} | 航点数: {}", template.getId(), template.getWaypointCount());
        }

        return template;
    }

    public boolean updateTemplate(DroneFlightTemplateDTO dto) {
        return saveTemplate(dto) != null;
    }

    public boolean deleteTemplate(Long id) {
        log.info("删除飞行模板 | 模板ID: {} | 操作人: {}", id, SecurityUtils.getUserName());
        return this.removeById(id);
    }

    public DroneFlightTemplate getTemplateById(Long id) {
        return super.getById(id);
    }

    public List<DroneFlightTemplate> listByUser() {
        String userName = SecurityUtils.getUserName();
        log.debug("查询用户模板列表 | 用户: {}", userName);
        return this.lambdaQuery()
                .eq(DroneFlightTemplate::getCreateBy, userName)
                .orderByDesc(DroneFlightTemplate::getCreateTime)
                .list();
    }

    public IPage<DroneFlightTemplate> pageList(String keyword) {
        String userName = SecurityUtils.getUserName();
        return this.lambdaQuery()
                .eq(DroneFlightTemplate::getCreateBy, userName)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(DroneFlightTemplate::getTemplateName, keyword)
                        .or()
                        .like(DroneFlightTemplate::getLocationName, keyword)
                        .or()
                        .like(DroneFlightTemplate::getTemplateDesc, keyword))
                .orderByDesc(DroneFlightTemplate::getCreateTime)
                .page(PageQuery.build().toPage());
    }
}

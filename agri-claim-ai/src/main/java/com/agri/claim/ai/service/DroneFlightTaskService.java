package com.agri.claim.ai.service;

import cn.hutool.core.util.IdUtil;
import com.agri.claim.ai.dto.DroneFlightTaskDTO;
import com.agri.claim.ai.entity.DroneFlightStatus;
import com.agri.claim.ai.entity.DroneFlightTask;
import com.agri.claim.ai.entity.DroneFlightTemplate;
import com.agri.claim.ai.mapper.DroneFlightTaskMapper;
import com.agri.claim.ai.util.WktUtil;
import com.agri.claim.assess.service.NotificationService;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DroneFlightTaskService extends ServiceImpl<DroneFlightTaskMapper, DroneFlightTask>
        implements IService<DroneFlightTask> {

    private final FlightRouteService flightRouteService;
    private final DroneFlightTemplateService templateService;
    private final DroneFlightStatusService statusService;
    private final NotificationService notificationService;

    @Transactional(rollbackFor = Exception.class)
    public DroneFlightTask createTask(DroneFlightTaskDTO dto) {
        log.info("创建飞行任务 | 任务名称: {} | 模板ID: {} | 创建人: {}",
                dto.getTaskName(), dto.getTemplateId(), SecurityUtils.getUserName());

        DroneFlightTask task = new DroneFlightTask();
        task.setTaskNo(generateTaskNo());
        task.setTaskName(dto.getTaskName());
        task.setTemplateId(dto.getTemplateId());
        task.setMissionId(dto.getMissionId());
        task.setMissionNo(dto.getMissionNo());
        task.setAircraftSn(dto.getAircraftSn());
        task.setAircraftModel(dto.getAircraftModel());
        task.setPayloadModel(dto.getPayloadModel());
        task.setPilotId(dto.getPilotId());
        task.setPilotName(dto.getPilotName() != null ? dto.getPilotName() : SecurityUtils.getUserName());
        task.setTakeoffLon(dto.getTakeoffLon());
        task.setTakeoffLat(dto.getTakeoffLat());
        task.setRemark(dto.getRemark());
        task.setFlightStatus(Constants.FLIGHT_STATUS_PENDING);
        task.setCreateBy(SecurityUtils.getUserName());

        List<FlightRouteService.LatLng> vertices = dto.getPolygonVertices();
        FlightRouteService.RoutePlan plan;

        if (dto.getTemplateId() != null) {
            DroneFlightTemplate template = templateService.getTemplateById(dto.getTemplateId());
            if (template == null) {
                throw new IllegalArgumentException("模板不存在: " + dto.getTemplateId());
            }

            task.setCenterLon(template.getCenterLon());
            task.setCenterLat(template.getCenterLat());
            task.setPolygonWkt(template.getPolygonWkt());
            task.setFlightHeight(template.getFlightHeight());
            task.setFrontOverlap(template.getFrontOverlap());
            task.setSideOverlap(template.getSideOverlap());
            task.setFlightSpeed(template.getFlightSpeed());
            task.setRoutePlanJson(template.getRoutePlanJson());

            if (vertices == null && template.getPolygonWkt() != null) {
                vertices = WktUtil.parsePolygon(template.getPolygonWkt());
            }
        }

        if (vertices == null && dto.getPolygonWkt() != null) {
            vertices = WktUtil.parsePolygon(dto.getPolygonWkt());
        }

        if (vertices != null && !vertices.isEmpty()) {
            FlightRouteService.RoutePlanRequest request = new FlightRouteService.RoutePlanRequest();
            request.setPolygonVertices(vertices);
            request.setFlightHeight(dto.getFlightHeight() != null ? dto.getFlightHeight().doubleValue() : 100.0);
            request.setFrontOverlap(dto.getFrontOverlap() != null ? dto.getFrontOverlap().doubleValue() : 80.0);
            request.setSideOverlap(dto.getSideOverlap() != null ? dto.getSideOverlap().doubleValue() : 60.0);
            request.setFlightSpeed(dto.getFlightSpeed() != null ? dto.getFlightSpeed().doubleValue() : 5.0);
            request.setObstacles(dto.getObstacles());
            request.setUserId(SecurityUtils.getUserId());
            request.setMissionId(dto.getMissionId());

            plan = flightRouteService.generateZigzagRoute(request);
            task.setRoutePlanJson(JSON.toJSONString(plan));

            if (task.getCenterLon() == null) {
                task.setCenterLon(dto.getCenterLon() != null ? dto.getCenterLon() : WktUtil.calculateCenterLon(vertices));
            }
            if (task.getCenterLat() == null) {
                task.setCenterLat(dto.getCenterLat() != null ? dto.getCenterLat() : WktUtil.calculateCenterLat(vertices));
            }
            task.setPolygonWkt(dto.getPolygonWkt() != null ? dto.getPolygonWkt() : WktUtil.toWktPolygon(vertices));
            task.setFlightHeight(dto.getFlightHeight() != null ? dto.getFlightHeight() : plan.getFlightHeight());
            task.setFrontOverlap(dto.getFrontOverlap() != null ? dto.getFrontOverlap() : plan.getFrontOverlap());
            task.setSideOverlap(dto.getSideOverlap() != null ? dto.getSideOverlap() : plan.getSideOverlap());
            task.setFlightSpeed(dto.getFlightSpeed() != null ? dto.getFlightSpeed() : BigDecimal.valueOf(5.0));
        }

        this.save(task);
        log.info("飞行任务创建成功 | 任务ID: {} | 任务编号: {}", task.getId(), task.getTaskNo());

        notifyUser(task, "TASK_CREATED", "飞行任务已创建，待执行");
        return task;
    }

    public boolean startTask(Long id) {
        DroneFlightTask task = getTaskById(id);
        validateStatusTransition(task.getFlightStatus(), Constants.FLIGHT_STATUS_FLYING);

        task.setFlightStatus(Constants.FLIGHT_STATUS_FLYING);
        task.setStartTime(LocalDateTime.now());
        task.setBatteryStart(100);

        boolean result = this.updateById(task);
        log.info("任务开始飞行 | 任务ID: {} | 任务编号: {}", id, task.getTaskNo());
        notifyUser(task, "TASK_STARTED", "无人机已起飞，开始执行任务");
        return result;
    }

    public boolean pauseTask(Long id) {
        DroneFlightTask task = getTaskById(id);
        validateStatusTransition(task.getFlightStatus(), Constants.FLIGHT_STATUS_PAUSED);

        task.setFlightStatus(Constants.FLIGHT_STATUS_PAUSED);
        boolean result = this.updateById(task);
        log.info("任务暂停 | 任务ID: {} | 任务编号: {}", id, task.getTaskNo());
        notifyUser(task, "TASK_PAUSED", "飞行任务已暂停");
        return result;
    }

    public boolean resumeTask(Long id) {
        DroneFlightTask task = getTaskById(id);
        validateStatusTransition(task.getFlightStatus(), Constants.FLIGHT_STATUS_FLYING);

        task.setFlightStatus(Constants.FLIGHT_STATUS_FLYING);
        boolean result = this.updateById(task);
        log.info("任务恢复 | 任务ID: {} | 任务编号: {}", id, task.getTaskNo());
        notifyUser(task, "TASK_RESUMED", "飞行任务已恢复");
        return result;
    }

    public boolean returnToHome(Long id) {
        DroneFlightTask task = getTaskById(id);
        validateStatusTransition(task.getFlightStatus(), Constants.FLIGHT_STATUS_RETURNING);

        task.setFlightStatus(Constants.FLIGHT_STATUS_RETURNING);
        boolean result = this.updateById(task);
        log.info("任务返航 | 任务ID: {} | 任务编号: {}", id, task.getTaskNo());
        notifyUser(task, "TASK_RETURNING", "无人机正在返航");
        return result;
    }

    public boolean landNow(Long id) {
        DroneFlightTask task = getTaskById(id);
        validateStatusTransition(task.getFlightStatus(), Constants.FLIGHT_STATUS_LANDING);

        task.setFlightStatus(Constants.FLIGHT_STATUS_LANDING);
        boolean result = this.updateById(task);
        log.info("任务降落 | 任务ID: {} | 任务编号: {}", id, task.getTaskNo());
        notifyUser(task, "TASK_LANDING", "无人机正在降落");
        return result;
    }

    public boolean cancelTask(Long id) {
        DroneFlightTask task = getTaskById(id);

        if (!List.of(Constants.FLIGHT_STATUS_PENDING, Constants.FLIGHT_STATUS_READY,
                Constants.FLIGHT_STATUS_PAUSED).contains(task.getFlightStatus())) {
            throw new IllegalStateException("当前状态不允许取消任务: " + task.getFlightStatus());
        }

        task.setFlightStatus(Constants.FLIGHT_STATUS_CANCELED);
        task.setEndTime(LocalDateTime.now());
        boolean result = this.updateById(task);
        log.info("任务取消 | 任务ID: {} | 任务编号: {}", id, task.getTaskNo());
        notifyUser(task, "TASK_CANCELED", "飞行任务已取消");
        return result;
    }

    public boolean completeTask(Long id) {
        DroneFlightTask task = getTaskById(id);
        validateStatusTransition(task.getFlightStatus(), Constants.FLIGHT_STATUS_COMPLETED);

        task.setFlightStatus(Constants.FLIGHT_STATUS_COMPLETED);
        task.setEndTime(LocalDateTime.now());
        task.setBatteryEnd(20);

        if (task.getStartTime() != null) {
            long minutes = Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
            task.setActualDuration(BigDecimal.valueOf(minutes));
        }

        boolean result = this.updateById(task);
        log.info("任务完成 | 任务ID: {} | 任务编号: {}", id, task.getTaskNo());
        notifyUser(task, "TASK_COMPLETED", "飞行任务已完成");
        return result;
    }

    public IPage<DroneFlightTask> pageList(String keyword, String status, Long missionId, Long pilotId) {
        return this.lambdaQuery()
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(DroneFlightTask::getTaskName, keyword)
                        .or()
                        .like(DroneFlightTask::getTaskNo, keyword))
                .eq(status != null && !status.isEmpty(), DroneFlightTask::getFlightStatus, status)
                .eq(missionId != null, DroneFlightTask::getMissionId, missionId)
                .eq(pilotId != null, DroneFlightTask::getPilotId, pilotId)
                .orderByDesc(DroneFlightTask::getCreateTime)
                .page(PageQuery.build().toPage());
    }

    public Map<String, Object> getDetail(Long id) {
        DroneFlightTask task = getTaskById(id);
        List<DroneFlightStatus> statusHistory = statusService.getStatusHistory(id, null, null);
        DroneFlightStatus latestStatus = statusService.getLatestStatus(id);

        return Map.of(
                "task", task,
                "latestStatus", latestStatus != null ? latestStatus : Map.of(),
                "statusCount", statusHistory.size()
        );
    }

    public DroneFlightTask getTaskById(Long id) {
        DroneFlightTask task = super.getById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }
        return task;
    }

    private String generateTaskNo() {
        return Constants.TASK_NO_PREFIX + IdUtil.getSnowflakeNextIdStr();
    }

    private void validateStatusTransition(String currentStatus, String targetStatus) {
        List<String> flyingStates = List.of(
                Constants.FLIGHT_STATUS_FLYING,
                Constants.FLIGHT_STATUS_PAUSED,
                Constants.FLIGHT_STATUS_RETURNING,
                Constants.FLIGHT_STATUS_LANDING
        );

        if (Constants.FLIGHT_STATUS_FLYING.equals(targetStatus)) {
            if (!List.of(Constants.FLIGHT_STATUS_PENDING, Constants.FLIGHT_STATUS_READY,
                    Constants.FLIGHT_STATUS_PAUSED).contains(currentStatus)) {
                throw new IllegalStateException("当前状态不允许开始飞行: " + currentStatus);
            }
        } else if (Constants.FLIGHT_STATUS_PAUSED.equals(targetStatus)) {
            if (!Constants.FLIGHT_STATUS_FLYING.equals(currentStatus)) {
                throw new IllegalStateException("当前状态不允许暂停: " + currentStatus);
            }
        } else if (Constants.FLIGHT_STATUS_RETURNING.equals(targetStatus)) {
            if (!flyingStates.contains(currentStatus)) {
                throw new IllegalStateException("当前状态不允许返航: " + currentStatus);
            }
        } else if (Constants.FLIGHT_STATUS_LANDING.equals(targetStatus)) {
            if (!flyingStates.contains(currentStatus)) {
                throw new IllegalStateException("当前状态不允许降落: " + currentStatus);
            }
        } else if (Constants.FLIGHT_STATUS_COMPLETED.equals(targetStatus)) {
            if (!List.of(Constants.FLIGHT_STATUS_FLYING, Constants.FLIGHT_STATUS_LANDING)
                    .contains(currentStatus)) {
                throw new IllegalStateException("当前状态不允许标记完成: " + currentStatus);
            }
        }
    }

    private void notifyUser(DroneFlightTask task, String type, String message) {
        try {
            Long userId = SecurityUtils.getUserId();
            if (userId != null) {
                notificationService.notifyAiProgress(
                        String.valueOf(userId),
                        task.getId(),
                        type,
                        0,
                        message
                );
            }
        } catch (Exception e) {
            log.warn("推送通知失败", e);
        }
    }
}

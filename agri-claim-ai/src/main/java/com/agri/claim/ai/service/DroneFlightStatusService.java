package com.agri.claim.ai.service;

import com.agri.claim.ai.dto.DroneFlightStatusDTO;
import com.agri.claim.ai.entity.DroneFlightStatus;
import com.agri.claim.ai.entity.DroneFlightTask;
import com.agri.claim.ai.mapper.DroneFlightStatusMapper;
import com.agri.claim.assess.service.NotificationService;
import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.utils.SecurityUtils;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DroneFlightStatusService extends ServiceImpl<DroneFlightStatusMapper, DroneFlightStatus>
        implements IService<DroneFlightStatus> {

    private final NotificationService notificationService;
    private final DroneFlightTaskService taskService;

    public DroneFlightStatus saveStatus(DroneFlightStatusDTO dto) {
        log.debug("接收无人机状态 | 任务ID: {} | 电量: {}% | 高度: {}m",
                dto.getTaskId(), dto.getBatteryPercent(), dto.getAircraftAltitude());

        DroneFlightStatus status = new DroneFlightStatus();
        status.setTaskId(dto.getTaskId());
        status.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : System.currentTimeMillis());
        status.setAircraftLon(dto.getAircraftLon());
        status.setAircraftLat(dto.getAircraftLat());
        status.setAircraftAltitude(dto.getAircraftAltitude());
        status.setAbsoluteAltitude(dto.getAbsoluteAltitude());
        status.setSpeedX(dto.getSpeedX());
        status.setSpeedY(dto.getSpeedY());
        status.setSpeedZ(dto.getSpeedZ());
        status.setGroundSpeed(dto.getGroundSpeed());
        status.setHeading(dto.getHeading());
        status.setPitch(dto.getPitch());
        status.setRoll(dto.getRoll());
        status.setYaw(dto.getYaw());
        status.setGimbalPitch(dto.getGimbalPitch());
        status.setGimbalYaw(dto.getGimbalYaw());
        status.setBatteryPercent(dto.getBatteryPercent());
        status.setBatteryVoltage(dto.getBatteryVoltage());
        status.setBatteryCurrent(dto.getBatteryCurrent());
        status.setBatteryTemperature(dto.getBatteryTemperature());
        status.setFlightMode(dto.getFlightMode());
        status.setCurrentWaypointIndex(dto.getCurrentWaypointIndex());
        status.setTotalWaypoints(dto.getTotalWaypoints());
        status.setDistanceToHome(dto.getDistanceToHome());
        status.setIsFlying(dto.getIsFlying());
        status.setIsReturningHome(dto.getIsReturningHome());
        status.setIsLanding(dto.getIsLanding());
        status.setIsTakingOff(dto.getIsTakingOff());
        status.setWarnings(dto.getWarnings());
        status.setErrors(dto.getErrors());
        status.setRawJson(dto.getRawJson() != null ? dto.getRawJson() : JSON.toJSONString(dto));

        this.save(status);

        pushStatusWebSocket(dto.getTaskId(), status);
        updateTaskStatusFromDrone(dto);

        return status;
    }

    public DroneFlightStatus getLatestStatus(Long taskId) {
        LambdaQueryWrapper<DroneFlightStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneFlightStatus::getTaskId, taskId)
                .orderByDesc(DroneFlightStatus::getTimestamp)
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    public List<DroneFlightStatus> getStatusHistory(Long taskId, Long startTime, Long endTime) {
        LambdaQueryWrapper<DroneFlightStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneFlightStatus::getTaskId, taskId)
                .ge(startTime != null, DroneFlightStatus::getTimestamp, startTime)
                .le(endTime != null, DroneFlightStatus::getTimestamp, endTime)
                .orderByAsc(DroneFlightStatus::getTimestamp);
        return this.list(wrapper);
    }

    private void pushStatusWebSocket(Long taskId, DroneFlightStatus status) {
        try {
            DroneFlightTask task = taskService.getTaskById(taskId);
            Long userId = SecurityUtils.getUserId();

            if (userId == null && task.getCreateBy() != null) {
                userId = getUserIdFromUsername(task.getCreateBy());
            }

            Map<String, Object> statusMap = new java.util.HashMap<>();
            statusMap.put("aircraftLon", status.getAircraftLon());
            statusMap.put("aircraftLat", status.getAircraftLat());
            statusMap.put("aircraftAltitude", status.getAircraftAltitude());
            statusMap.put("absoluteAltitude", status.getAbsoluteAltitude());
            statusMap.put("groundSpeed", status.getGroundSpeed());
            statusMap.put("speedX", status.getSpeedX());
            statusMap.put("speedY", status.getSpeedY());
            statusMap.put("speedZ", status.getSpeedZ());
            statusMap.put("heading", status.getHeading());
            statusMap.put("pitch", status.getPitch());
            statusMap.put("roll", status.getRoll());
            statusMap.put("yaw", status.getYaw());
            statusMap.put("gimbalPitch", status.getGimbalPitch());
            statusMap.put("gimbalYaw", status.getGimbalYaw());
            statusMap.put("batteryPercent", status.getBatteryPercent());
            statusMap.put("batteryVoltage", status.getBatteryVoltage());
            statusMap.put("batteryCurrent", status.getBatteryCurrent());
            statusMap.put("batteryTemperature", status.getBatteryTemperature());
            statusMap.put("flightMode", status.getFlightMode());
            statusMap.put("currentWaypointIndex", status.getCurrentWaypointIndex());
            statusMap.put("totalWaypoints", status.getTotalWaypoints());
            statusMap.put("distanceToHome", status.getDistanceToHome());
            statusMap.put("isFlying", status.getIsFlying());
            statusMap.put("isReturningHome", status.getIsReturningHome());
            statusMap.put("isLanding", status.getIsLanding());
            statusMap.put("isTakingOff", status.getIsTakingOff());

            if (userId != null) {
                notificationService.notifyDroneStatus(userId, taskId, statusMap);
            }
            notificationService.broadcastDroneStatus(taskId, statusMap);

        } catch (Exception e) {
            log.debug("WebSocket推送失败", e);
        }
    }

    private void updateTaskStatusFromDrone(DroneFlightStatusDTO dto) {
        try {
            DroneFlightTask task = taskService.getTaskById(dto.getTaskId());
            String currentStatus = task.getFlightStatus();

            if (dto.getIsLanding() != null && dto.getIsLanding() == 1) {
                if (!Constants.FLIGHT_STATUS_LANDING.equals(currentStatus)) {
                    task.setFlightStatus(Constants.FLIGHT_STATUS_LANDING);
                    taskService.updateById(task);
                    log.info("无人机状态更新为降落中 | 任务ID: {}", dto.getTaskId());
                }
            } else if (dto.getIsReturningHome() != null && dto.getIsReturningHome() == 1) {
                if (!Constants.FLIGHT_STATUS_RETURNING.equals(currentStatus)) {
                    task.setFlightStatus(Constants.FLIGHT_STATUS_RETURNING);
                    taskService.updateById(task);
                    log.info("无人机状态更新为返航中 | 任务ID: {}", dto.getTaskId());
                }
            } else if (dto.getIsFlying() != null && dto.getIsFlying() == 1) {
                if (!List.of(Constants.FLIGHT_STATUS_FLYING, Constants.FLIGHT_STATUS_PAUSED)
                        .contains(currentStatus)) {
                    task.setFlightStatus(Constants.FLIGHT_STATUS_FLYING);
                    taskService.updateById(task);
                    log.info("无人机状态更新为飞行中 | 任务ID: {}", dto.getTaskId());
                }
            }

            if (dto.getBatteryPercent() != null) {
                if (dto.getIsFlying() != null && dto.getIsFlying() == 0
                        && dto.getIsLanding() != null && dto.getIsLanding() == 0) {
                    task.setBatteryEnd(dto.getBatteryPercent());
                    taskService.updateById(task);
                }
            }
        } catch (Exception e) {
            log.warn("更新任务状态失败", e);
        }
    }

    private int calculateProgress(DroneFlightStatus status) {
        if (status.getCurrentWaypointIndex() != null && status.getTotalWaypoints() != null
                && status.getTotalWaypoints() > 0) {
            return (int) ((status.getCurrentWaypointIndex() * 100.0) / status.getTotalWaypoints());
        }
        return 0;
    }

    private Long getUserIdFromUsername(String username) {
        try {
            return Long.parseLong(username);
        } catch (Exception e) {
            return null;
        }
    }
}

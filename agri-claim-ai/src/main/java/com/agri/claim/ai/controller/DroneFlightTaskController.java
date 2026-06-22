package com.agri.claim.ai.controller;

import com.agri.claim.ai.dto.DroneFlightStatusDTO;
import com.agri.claim.ai.dto.DroneFlightTaskDTO;
import com.agri.claim.ai.entity.DroneFlightStatus;
import com.agri.claim.ai.entity.DroneFlightTask;
import com.agri.claim.ai.service.DroneFlightStatusService;
import com.agri.claim.ai.service.DroneFlightTaskService;
import com.agri.claim.common.core.page.PageResult;
import com.agri.claim.common.result.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "无人机飞行任务")
@RestController
@RequestMapping("/ai/drone/task")
@RequiredArgsConstructor
public class DroneFlightTaskController {

    private final DroneFlightTaskService taskService;
    private final DroneFlightStatusService statusService;

    @Operation(summary = "创建飞行任务")
    @PostMapping("/create")
    public R<DroneFlightTask> create(@Valid @RequestBody DroneFlightTaskDTO dto) {
        DroneFlightTask task = taskService.createTask(dto);
        return R.ok("任务创建成功", task);
    }

    @Operation(summary = "开始飞行任务")
    @PostMapping("/{id}/start")
    public R<Void> start(@PathVariable Long id) {
        boolean result = taskService.startTask(id);
        return result ? R.ok("任务已开始") : R.fail("任务开始失败");
    }

    @Operation(summary = "暂停飞行任务")
    @PostMapping("/{id}/pause")
    public R<Void> pause(@PathVariable Long id) {
        boolean result = taskService.pauseTask(id);
        return result ? R.ok("任务已暂停") : R.fail("任务暂停失败");
    }

    @Operation(summary = "恢复飞行任务")
    @PostMapping("/{id}/resume")
    public R<Void> resume(@PathVariable Long id) {
        boolean result = taskService.resumeTask(id);
        return result ? R.ok("任务已恢复") : R.fail("任务恢复失败");
    }

    @Operation(summary = "无人机返航")
    @PostMapping("/{id}/return")
    public R<Void> returnToHome(@PathVariable Long id) {
        boolean result = taskService.returnToHome(id);
        return result ? R.ok("无人机正在返航") : R.fail("返航指令失败");
    }

    @Operation(summary = "立即降落")
    @PostMapping("/{id}/land")
    public R<Void> landNow(@PathVariable Long id) {
        boolean result = taskService.landNow(id);
        return result ? R.ok("无人机正在降落") : R.fail("降落指令失败");
    }

    @Operation(summary = "取消飞行任务")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        boolean result = taskService.cancelTask(id);
        return result ? R.ok("任务已取消") : R.fail("任务取消失败");
    }

    @Operation(summary = "完成飞行任务")
    @PostMapping("/{id}/complete")
    public R<Void> complete(@PathVariable Long id) {
        boolean result = taskService.completeTask(id);
        return result ? R.ok("任务已完成") : R.fail("任务完成失败");
    }

    @Operation(summary = "接收无人机状态上报")
    @PostMapping("/status/report")
    public R<DroneFlightStatus> reportStatus(@Valid @RequestBody DroneFlightStatusDTO dto) {
        DroneFlightStatus status = statusService.saveStatus(dto);
        return R.ok("状态已接收", status);
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    public R<Map<String, Object>> getDetail(@PathVariable Long id) {
        Map<String, Object> detail = taskService.getDetail(id);
        return R.ok(detail);
    }

    @Operation(summary = "获取任务最新状态")
    @GetMapping("/{id}/status/latest")
    public R<DroneFlightStatus> getLatestStatus(@PathVariable Long id) {
        DroneFlightStatus status = statusService.getLatestStatus(id);
        return R.ok(status);
    }

    @Operation(summary = "获取任务状态历史")
    @GetMapping("/{id}/status/history")
    public R<List<DroneFlightStatus>> getStatusHistory(
            @PathVariable Long id,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        List<DroneFlightStatus> history = statusService.getStatusHistory(id, startTime, endTime);
        return R.ok(history);
    }

    @Operation(summary = "分页查询飞行任务")
    @GetMapping("/page")
    public R<PageResult<DroneFlightTask>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long missionId,
            @RequestParam(required = false) Long pilotId) {
        IPage<DroneFlightTask> page = taskService.pageList(keyword, status, missionId, pilotId);
        return R.ok(PageResult.of(page));
    }
}

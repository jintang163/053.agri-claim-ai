package com.agri.claim.ai.controller;

import com.agri.claim.ai.dto.AiProcessDTO;
import com.agri.claim.ai.entity.ChangeDetectResult;
import com.agri.claim.ai.entity.SegmentResult;
import com.agri.claim.ai.service.ChangeDetectService;
import com.agri.claim.ai.service.SegmentService;
import com.agri.claim.common.core.page.PageQuery;
import com.agri.claim.common.core.page.PageResult;
import com.agri.claim.common.result.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "AI智能处理")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final SegmentService segmentService;
    private final ChangeDetectService changeDetectService;

    @Operation(summary = "农田地块分割（UNet++）")
    @PostMapping("/segment")
    public R<List<SegmentResult>> segment(@Valid @RequestBody AiProcessDTO dto) {
        return R.ok("分割完成", segmentService.segmentFarmland(dto));
    }

    @Operation(summary = "变化检测与受灾识别")
    @PostMapping("/detect")
    public R<ChangeDetectResult> detect(@Valid @RequestBody AiProcessDTO dto) {
        return R.ok("检测完成", changeDetectService.detectChanges(dto));
    }

    @Operation(summary = "一键智能定损（分割+变化检测）")
    @PostMapping("/process")
    public R<Map<String, Object>> fullProcess(@Valid @RequestBody AiProcessDTO dto) {
        List<SegmentResult> segments = segmentService.segmentFarmland(dto);
        ChangeDetectResult detect = changeDetectService.detectChanges(dto);
        Map<String, BigDecimal> classStats = segmentService.getClassAreaStats(dto.getTaskId());
        Map<String, Object> result = Map.of(
                "segments", segments,
                "segmentCount", segments.size(),
                "classAreaStats", classStats,
                "changeDetect", detect,
                "summary", changeDetectService.getDisasterSummary(dto.getTaskId())
        );
        return R.ok("智能定损完成", result);
    }

    @Operation(summary = "获取任务分割结果")
    @GetMapping("/segment/task/{taskId}")
    public R<List<SegmentResult>> getSegments(@PathVariable Long taskId) {
        return R.ok(segmentService.getByTaskId(taskId));
    }

    @Operation(summary = "获取任务农田分割结果")
    @GetMapping("/segment/farmland/{taskId}")
    public R<List<SegmentResult>> getFarmlands(@PathVariable Long taskId) {
        return R.ok(segmentService.getFarmlandByTaskId(taskId));
    }

    @Operation(summary = "获取任务变化检测结果")
    @GetMapping("/detect/task/{taskId}")
    public R<ChangeDetectResult> getDetect(@PathVariable Long taskId) {
        return R.ok(changeDetectService.getByTaskId(taskId));
    }

    @Operation(summary = "获取任务灾害汇总")
    @GetMapping("/summary/{taskId}")
    public R<Map<String, Object>> getSummary(@PathVariable Long taskId) {
        return R.ok(changeDetectService.getDisasterSummary(taskId));
    }

    @Operation(summary = "分页查询分割结果")
    @GetMapping("/segment/list")
    public R<PageResult<SegmentResult>> listSegments(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String segmentClass) {
        IPage<SegmentResult> page = segmentService.lambdaQuery()
                .eq(taskId != null, SegmentResult::getTaskId, taskId)
                .eq(segmentClass != null, SegmentResult::getSegmentClass, segmentClass)
                .orderByDesc(SegmentResult::getCreateTime)
                .page(PageQuery.build().toPage());
        return R.ok(PageResult.of(page));
    }

    @Operation(summary = "分页查询检测结果")
    @GetMapping("/detect/list")
    public R<PageResult<ChangeDetectResult>> listDetects(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String disasterType,
            @RequestParam(required = false) String disasterLevel) {
        IPage<ChangeDetectResult> page = changeDetectService.lambdaQuery()
                .eq(taskId != null, ChangeDetectResult::getTaskId, taskId)
                .eq(disasterType != null, ChangeDetectResult::getDisasterType, disasterType)
                .eq(disasterLevel != null, ChangeDetectResult::getDisasterLevel, disasterLevel)
                .orderByDesc(ChangeDetectResult::getCreateTime)
                .page(PageQuery.build().toPage());
        return R.ok(PageResult.of(page));
    }

    @Operation(summary = "分割类别面积统计")
    @GetMapping("/stats/class/{taskId}")
    public R<Map<String, BigDecimal>> classStats(@PathVariable Long taskId) {
        return R.ok(segmentService.getClassAreaStats(taskId));
    }
}

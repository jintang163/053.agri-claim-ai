package com.agri.claim.assess.controller;

import com.agri.claim.assess.dto.AssessMissionDTO;
import com.agri.claim.assess.entity.AssessDetail;
import com.agri.claim.assess.entity.AssessMission;
import com.agri.claim.assess.mapper.AssessDetailMapper;
import com.agri.claim.assess.service.AssessService;
import com.agri.claim.common.core.page.PageResult;
import com.agri.claim.common.result.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "定损评估")
@RestController
@RequestMapping("/assess")
@RequiredArgsConstructor
public class AssessController {

    private final AssessService assessService;
    private final AssessDetailMapper detailMapper;

    @Operation(summary = "修正地块受灾边界")
    @PutMapping("/detail/{id}/boundary")
    public R<AssessDetail> updateBoundary(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        AssessDetail detail = detailMapper.selectById(id);
        if (detail == null) return R.fail("明细不存在");
        String polygonWkt = body.get("polygonWkt");
        if (polygonWkt != null && !polygonWkt.isBlank()) {
            detail.setPolygonWkt(polygonWkt);
            detailMapper.updateById(detail);
        }
        return R.ok("边界修正成功", detail);
    }

    @Operation(summary = "批量修正地块受灾边界")
    @PutMapping("/mission/{missionId}/boundaries")
    public R<Void> batchUpdateBoundaries(
            @PathVariable Long missionId,
            @RequestBody List<Map<String, Object>> boundaries) {
        for (Map<String, Object> item : boundaries) {
            Object idObj = item.get("detailId");
            Object wktObj = item.get("polygonWkt");
            if (idObj != null && wktObj != null) {
                AssessDetail detail = detailMapper.selectById(Long.valueOf(idObj.toString()));
                if (detail != null) {
                    detail.setPolygonWkt(wktObj.toString());
                    detailMapper.updateById(detail);
                }
            }
        }
        return R.ok("批量边界修正成功");
    }

    @Operation(summary = "创建定损任务（一键智能定损）")
    @PostMapping("/mission")
    public R<AssessMission> createMission(@Valid @RequestBody AssessMissionDTO dto) {
        return R.ok("定损任务创建成功", assessService.createMission(dto));
    }

    @Operation(summary = "获取定损任务详情")
    @GetMapping("/mission/{id}")
    public R<AssessMission> getMission(@PathVariable Long id) {
        AssessMission m = assessService.getMissionDetail(id);
        if (m == null) return R.fail("定损任务不存在");
        return R.ok(m);
    }

    @Operation(summary = "分页查询定损任务")
    @GetMapping("/mission/list")
    public R<PageResult<AssessMission>> listMission(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String assessStatus,
            @RequestParam(required = false) String disasterType,
            @RequestParam(required = false) String missionNo,
            @RequestParam(required = false) String keyword) {
        IPage<AssessMission> page = assessService.pageList(pageNum, pageSize, assessStatus,
                disasterType, missionNo, keyword);
        return R.ok(PageResult.of(page));
    }

    @Operation(summary = "重新计算赔付（调整系数）")
    @PostMapping("/mission/{id}/recalc")
    public R<AssessMission> recalc(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal globalAdjust,
            @RequestBody(required = false) Map<Long, BigDecimal> detailAdjusts) {
        return R.ok("重新计算完成", assessService.recalculate(id, globalAdjust, detailAdjusts));
    }

    @Operation(summary = "审核定损任务")
    @PostMapping("/mission/{id}/audit")
    public R<AssessMission> audit(
            @PathVariable Long id,
            @RequestParam Integer passed,
            @RequestParam(required = false) String remark) {
        return R.ok(passed == 1 ? "审核通过" : "审核驳回",
                assessService.auditMission(id, passed, remark));
    }

    @Operation(summary = "下载定损报告PDF")
    @SneakyThrows
    @GetMapping("/mission/{id}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
        AssessMission m = assessService.getById(id);
        byte[] pdf = assessService.downloadReport(id);
        String fileName = (m != null && m.getReportNo() != null ? m.getReportNo() : "定损报告_" + id) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + new String(fileName.getBytes("GBK"), "ISO-8859-1") + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @Operation(summary = "获取赔付明细列表")
    @GetMapping("/mission/{missionId}/details")
    public R<List<AssessDetail>> getDetails(@PathVariable Long missionId) {
        AssessMission m = assessService.getMissionDetail(missionId);
        return R.ok(m != null ? m.getDetails() : List.of());
    }

    @Operation(summary = "调整单项赔付明细")
    @PostMapping("/detail/{id}/adjust")
    public R<AssessMission> adjustDetail(
            @PathVariable Long id,
            @RequestParam Long missionId,
            @RequestParam BigDecimal adjustCoeff) {
        Map<Long, BigDecimal> map = new HashMap<>();
        map.put(id, adjustCoeff);
        return R.ok("调整完成", assessService.recalculate(missionId, null, map));
    }

    @Operation(summary = "大屏统计数据")
    @GetMapping("/dashboard/stats")
    public R<Map<String, Object>> dashboardStats() {
        return R.ok(assessService.getDashboardStats());
    }

    @Operation(summary = "赔付计算公式说明")
    @GetMapping("/formula")
    public R<Map<String, Object>> getFormula() {
        Map<String, Object> result = new HashMap<>();
        result.put("formula", "赔付额 = 受灾面积 × 亩产标准 × 单价 × 赔付比例 × 受灾系数 × 调整系数");
        result.put("variables", Map.of(
                "受灾面积", "亩（通过AI变化检测识别）",
                "亩产标准", "公斤/亩（根据作物类型确定）",
                "单价", "元/公斤（根据市场行情）",
                "赔付比例", "（根据保险条款，一般70%-85%）",
                "受灾系数", "（轻度0.3，中度0.6，重度0.95）",
                "调整系数", "（查勘员/审核员可手动调整）"
        ));
        result.put("disasterCoefficients", Map.of(
                "轻度受灾(LIGHT)", 0.30,
                "中度受灾(MODERATE)", 0.60,
                "重度受灾(SEVERE)", 0.95
        ));
        return R.ok(result);
    }
}

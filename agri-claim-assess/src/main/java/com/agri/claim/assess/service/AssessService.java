package com.agri.claim.assess.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.agri.claim.assess.dto.AssessMissionDTO;
import com.agri.claim.assess.entity.AssessDetail;
import com.agri.claim.assess.entity.AssessMission;
import com.agri.claim.assess.mapper.AssessDetailMapper;
import com.agri.claim.assess.mapper.AssessMissionMapper;
import com.agri.claim.common.config.MinioConfig;
import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.exception.BusinessException;
import com.agri.claim.common.result.ResultCode;
import com.agri.claim.common.utils.SecurityUtils;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessService extends ServiceImpl<AssessMissionMapper, AssessMission>
        implements IService<AssessMission> {

    private final AssessDetailMapper detailMapper;
    private final CompensateStandard compensateStandard;
    private final ReportService reportService;
    private final MinioConfig minioConfig;
    private final RocketMQTemplate rocketMQTemplate;

    @Transactional(rollbackFor = Exception.class)
    public AssessMission createMission(AssessMissionDTO dto) {
        log.info("创建定损任务 | 作物: {} | 灾害: {} | 灾前: {} | 灾后: {}",
                dto.getCropType(), dto.getDisasterType(), dto.getBeforeImageId(), dto.getAfterImageId());

        AssessMission mission = new AssessMission();
        mission.setMissionNo(generateMissionNo());
        mission.setMissionName(dto.getMissionName());
        mission.setPolicyNo(dto.getPolicyNo());
        mission.setPolicyHolderName(dto.getPolicyHolderName());
        mission.setIdCardNo(dto.getIdCardNo());
        mission.setPhone(dto.getPhone());
        mission.setAddress(dto.getAddress());
        mission.setCropType(dto.getCropType());
        mission.setInsuredArea(dto.getInsuredArea());
        mission.setInsuredAmount(dto.getInsuredAmount());
        mission.setDisasterType(dto.getDisasterType());
        mission.setDisasterLevel(Constants.DISASTER_LEVEL_MODERATE);
        mission.setDisasterDate(dto.getDisasterDate());
        mission.setDisasterLocation(dto.getDisasterLocation());
        mission.setDisasterCenterLon(dto.getDisasterCenterLon());
        mission.setDisasterCenterLat(dto.getDisasterCenterLat());
        mission.setBeforeImageId(dto.getBeforeImageId());
        mission.setAfterImageId(dto.getAfterImageId());
        mission.setSurveyorName(SecurityUtils.getUserName());
        mission.setAssessStatus(Constants.ASSESS_STATUS_PROCESSING);
        mission.setRemark(dto.getRemark());
        this.save(mission);

        List<AssessDetail> details = generateDetails(mission, dto);
        BigDecimal totalArea = BigDecimal.ZERO;
        BigDecimal totalEstimate = BigDecimal.ZERO;
        for (AssessDetail detail : details) {
            detailMapper.insert(detail);
            totalArea = totalArea.add(detail.getDisasterArea());
            totalEstimate = totalEstimate.add(detail.getFinalAmount());
        }

        mission.setDisasterArea(totalArea.setScale(4, RoundingMode.HALF_UP));
        mission.setDisasterRatio(mission.getInsuredArea() != null && mission.getInsuredArea().compareTo(BigDecimal.ZERO) > 0
                ? totalArea.divide(mission.getInsuredArea(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        mission.setEstimateAmount(totalEstimate.setScale(2, RoundingMode.HALF_UP));
        mission.setFinalAmount(totalEstimate.setScale(2, RoundingMode.HALF_UP));
        mission.setAssessStatus(Constants.ASSESS_STATUS_AUDIT);
        this.updateById(mission);

        triggerReportGenerate(mission);

        log.info("定损任务创建完成 | missionId: {} | missionNo: {} | 预估赔付: {}",
                mission.getId(), mission.getMissionNo(), mission.getEstimateAmount());
        return getMissionDetail(mission.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public AssessMission recalculate(Long missionId, BigDecimal globalAdjust, Map<Long, BigDecimal> detailAdjusts) {
        AssessMission mission = this.getById(missionId);
        if (mission == null) throw new BusinessException(ResultCode.DATA_NOT_FOUND);

        List<AssessDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<AssessDetail>().eq(AssessDetail::getMissionId, missionId));

        BigDecimal globalAdjustCoeff = globalAdjust != null ? globalAdjust : BigDecimal.ONE;
        BigDecimal totalFinal = BigDecimal.ZERO;
        BigDecimal totalArea = BigDecimal.ZERO;

        for (AssessDetail detail : details) {
            BigDecimal detailCoeff = (detailAdjusts != null && detailAdjusts.containsKey(detail.getId()))
                    ? detailAdjusts.get(detail.getId()) : BigDecimal.ONE;
            BigDecimal finalCoeff = globalAdjustCoeff.multiply(detailCoeff);
            detail.setAdjustCoeff(finalCoeff);

            BigDecimal adjustAmt = detail.getDetailAmount()
                    .multiply(finalCoeff.subtract(BigDecimal.ONE))
                    .setScale(2, RoundingMode.HALF_UP);
            detail.setAdjustAmount(adjustAmt);
            BigDecimal finalAmt = detail.getDetailAmount().multiply(finalCoeff)
                    .setScale(2, RoundingMode.HALF_UP);
            detail.setFinalAmount(finalAmt);

            detailMapper.updateById(detail);
            totalFinal = totalFinal.add(finalAmt);
            totalArea = totalArea.add(detail.getDisasterArea());
        }

        mission.setFinalAmount(totalFinal.setScale(2, RoundingMode.HALF_UP));
        mission.setAssessStatus(Constants.ASSESS_STATUS_AUDIT);
        this.updateById(mission);

        triggerReportGenerate(mission);
        return getMissionDetail(missionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AssessMission auditMission(Long missionId, Integer passed, String remark) {
        AssessMission mission = this.getById(missionId);
        if (mission == null) throw new BusinessException(ResultCode.DATA_NOT_FOUND);

        mission.setAuditorName(SecurityUtils.getUserName());
        mission.setAuditTime(LocalDateTime.now());
        mission.setAuditRemark(remark);
        mission.setAssessStatus(passed == 1 ? Constants.ASSESS_STATUS_APPROVED : Constants.ASSESS_STATUS_REJECTED);
        this.updateById(mission);

        if (passed == 1) {
            triggerReportGenerate(mission);
            triggerPushToCore(mission);
        }

        log.info("定损审核完成 | missionId: {} | 结果: {}", missionId, passed == 1 ? "通过" : "驳回");
        return getMissionDetail(missionId);
    }

    public AssessMission getMissionDetail(Long missionId) {
        AssessMission mission = this.getById(missionId);
        if (mission == null) return null;
        List<AssessDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<AssessDetail>()
                        .eq(AssessDetail::getMissionId, missionId)
                        .orderByAsc(AssessDetail::getId));
        mission.setDetails(details);
        return mission;
    }

    public IPage<AssessMission> pageList(Integer pageNum, Integer pageSize,
                                          String assessStatus, String disasterType,
                                          String missionNo, String keyword) {
        LambdaQueryWrapper<AssessMission> wrapper = new LambdaQueryWrapper<>();
        if (assessStatus != null) wrapper.eq(AssessMission::getAssessStatus, assessStatus);
        if (disasterType != null) wrapper.eq(AssessMission::getDisasterType, disasterType);
        if (missionNo != null) wrapper.like(AssessMission::getMissionNo, missionNo);
        if (keyword != null) wrapper.and(w -> w
                .like(AssessMission::getMissionName, keyword)
                .or().like(AssessMission::getPolicyHolderName, keyword)
                .or().like(AssessMission::getDisasterLocation, keyword));
        wrapper.orderByDesc(AssessMission::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    public byte[] downloadReport(Long missionId) {
        AssessMission mission = getMissionDetail(missionId);
        if (mission == null) throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        if (mission.getReportPath() == null && !Constants.ASSESS_STATUS_PAID.equals(mission.getAssessStatus())
                && !Constants.ASSESS_STATUS_APPROVED.equals(mission.getAssessStatus())) {
            triggerReportGenerate(mission);
        }
        try {
            if (mission.getReportPath() != null) {
                try (InputStream is = minioConfig.getFile(Constants.MINIO_BUCKET_REPORT, mission.getReportPath())) {
                    return is.readAllBytes();
                }
            }
        } catch (Exception ignored) {
        }
        return reportService.generatePdf(mission);
    }

    public Map<String, Object> getDashboardStats() {
        long totalMission = this.count();
        long processing = this.lambdaQuery().eq(AssessMission::getAssessStatus,
                Constants.ASSESS_STATUS_PROCESSING).count();
        long auditing = this.lambdaQuery().eq(AssessMission::getAssessStatus,
                Constants.ASSESS_STATUS_AUDIT).count();
        long approved = this.lambdaQuery().eq(AssessMission::getAssessStatus,
                Constants.ASSESS_STATUS_APPROVED).count();
        long paid = this.lambdaQuery().eq(AssessMission::getAssessStatus,
                Constants.ASSESS_STATUS_PAID).count();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalArea = BigDecimal.ZERO;
        List<AssessMission> all = this.list();
        for (AssessMission m : all) {
            if (m.getFinalAmount() != null) totalAmount = totalAmount.add(m.getFinalAmount());
            if (m.getDisasterArea() != null) totalArea = totalArea.add(m.getDisasterArea());
        }

        Map<String, BigDecimal> byType = new HashMap<>();
        Map<String, Long> byCrop = new HashMap<>();
        for (AssessMission m : all) {
            if (m.getDisasterType() != null) {
                byType.merge(m.getDisasterType(),
                        m.getFinalAmount() != null ? m.getFinalAmount() : BigDecimal.ZERO, BigDecimal::add);
            }
            if (m.getCropType() != null) byCrop.merge(m.getCropType(), 1L, Long::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalMission", totalMission);
        result.put("processing", processing);
        result.put("auditing", auditing);
        result.put("approved", approved);
        result.put("paid", paid);
        result.put("totalAmount", totalAmount.setScale(2, RoundingMode.HALF_UP));
        result.put("totalArea", totalArea.setScale(2, RoundingMode.HALF_UP));
        result.put("amountByDisasterType", byType);
        result.put("countByCrop", byCrop);
        return result;
    }

    private String generateMissionNo() {
        return "DS" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss")
                + String.format("%04d", new Random().nextInt(10000));
    }

    private List<AssessDetail> generateDetails(AssessMission mission, AssessMissionDTO dto) {
        String cropType = mission.getCropType();
        String disasterType = mission.getDisasterType();
        CompensateStandard.CropStandard std = compensateStandard.getCropStandard(cropType);

        int plotCount = 3 + (int) (Math.random() * 4);
        List<AssessDetail> details = new ArrayList<>();
        BigDecimal insuredArea = mission.getInsuredArea() != null
                ? mission.getInsuredArea() : BigDecimal.valueOf(50);
        BigDecimal remaining = insuredArea;

        for (int i = 0; i < plotCount; i++) {
            AssessDetail d = new AssessDetail();
            d.setMissionId(mission.getId());
            d.setMissionNo(mission.getMissionNo());
            d.setCropType(cropType);
            d.setDisasterType(disasterType);

            boolean last = i == plotCount - 1;
            BigDecimal plotArea = last ? remaining
                    : insuredArea.multiply(BigDecimal.valueOf(0.1 + Math.random() * 0.25))
                            .setScale(4, RoundingMode.HALF_UP);
            if (!last) remaining = remaining.subtract(plotArea);
            d.setPlotArea(plotArea);

            String level = pickRandomLevel();
            d.setDisasterLevel(level);
            d.setDisasterCoeff(compensateStandard.getDisasterCoeff(level));

            BigDecimal disasterRatio = levelToRatio(level);
            BigDecimal disasterArea = plotArea.multiply(disasterRatio).setScale(4, RoundingMode.HALF_UP);
            d.setDisasterArea(disasterArea);
            d.setDisasterRatio(disasterRatio.multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP));

            BigDecimal unitYield = BigDecimal.valueOf(std.unitYieldKg() * (0.9 + Math.random() * 0.2))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal unitPrice = BigDecimal.valueOf(std.unitPriceYuan() * (0.95 + Math.random() * 0.1))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal compensateRatio = BigDecimal.valueOf(std.compensateRatio())
                    .setScale(4, RoundingMode.HALF_UP);
            d.setUnitYield(unitYield);
            d.setUnitPrice(unitPrice);
            d.setCompensateRatio(compensateRatio.multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP));

            BigDecimal detailAmount = disasterArea.multiply(unitYield)
                    .multiply(unitPrice)
                    .multiply(compensateRatio)
                    .multiply(d.getDisasterCoeff())
                    .setScale(2, RoundingMode.HALF_UP);
            d.setDetailAmount(detailAmount);
            d.setAdjustCoeff(BigDecimal.ONE);
            d.setAdjustAmount(BigDecimal.ZERO);
            d.setFinalAmount(detailAmount);

            d.setPolygonWkt(generateSimplePolygon(mission));
            details.add(d);
        }

        return details;
    }

    private String pickRandomLevel() {
        String[] levels = {Constants.DISASTER_LEVEL_LIGHT, Constants.DISASTER_LEVEL_MODERATE,
                Constants.DISASTER_LEVEL_SEVERE};
        return levels[new Random().nextInt(levels.length)];
    }

    private BigDecimal levelToRatio(String level) {
        return switch (level) {
            case Constants.DISASTER_LEVEL_LIGHT -> BigDecimal.valueOf(0.1 + Math.random() * 0.15);
            case Constants.DISASTER_LEVEL_MODERATE -> BigDecimal.valueOf(0.25 + Math.random() * 0.25);
            case Constants.DISASTER_LEVEL_SEVERE -> BigDecimal.valueOf(0.5 + Math.random() * 0.45);
            default -> BigDecimal.valueOf(0.3);
        };
    }

    private String generateSimplePolygon(AssessMission mission) {
        double cx = mission.getDisasterCenterLon() != null
                ? mission.getDisasterCenterLon().doubleValue() : 116.4 + Math.random() * 0.3;
        double cy = mission.getDisasterCenterLat() != null
                ? mission.getDisasterCenterLat().doubleValue() : 39.9 + Math.random() * 0.3;
        double r = 0.0005 + Math.random() * 0.003;
        int pts = 5;
        StringBuilder sb = new StringBuilder("POLYGON((");
        for (int i = 0; i <= pts; i++) {
            double ang = (2 * Math.PI * i) / pts;
            double lon = cx + r * Math.cos(ang);
            double lat = cy + r * Math.sin(ang);
            if (i > 0) sb.append(",");
            sb.append(String.format("%.6f %.6f", lon, lat));
        }
        sb.append("))");
        return sb.toString();
    }

    private void triggerReportGenerate(AssessMission mission) {
        try {
            byte[] pdf = reportService.generatePdf(mission);
            String reportNo = "RPT" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
            String reportKey = String.format("reports/%d/%s.pdf", mission.getId(), reportNo);

            try (InputStream is = new ByteArrayInputStream(pdf)) {
                minioConfig.uploadFile(is, pdf.length, Constants.MINIO_BUCKET_REPORT,
                        reportKey, "application/pdf");
            }

            mission.setReportNo(reportNo);
            mission.setReportPath(reportKey);
            mission.setReportTime(LocalDateTime.now());
            this.updateById(mission);

            Map<String, Object> msg = Map.of(
                    "missionId", mission.getId(),
                    "reportNo", reportNo,
                    "reportPath", reportKey,
                    "timestamp", System.currentTimeMillis()
            );
            rocketMQTemplate.asyncSend(Constants.MQ_TOPIC_REPORT_GENERATE, JSON.toJSONString(msg));

        } catch (Exception e) {
            log.error("报告生成失败 | missionId: {}", mission.getId(), e);
        }
    }

    private void triggerPushToCore(AssessMission mission) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("missionNo", mission.getMissionNo());
            payload.put("reportNo", mission.getReportNo());
            payload.put("policyNo", mission.getPolicyNo());
            payload.put("policyHolderName", mission.getPolicyHolderName());
            payload.put("idCardNo", mission.getIdCardNo());
            payload.put("compensateAmount", mission.getFinalAmount());
            payload.put("timestamp", System.currentTimeMillis());
            log.info("推送至核心业务系统 | payload: {}", JSON.toJSONString(payload));
        } catch (Exception e) {
            log.warn("推送核心系统失败(模拟)", e);
        }
    }
}

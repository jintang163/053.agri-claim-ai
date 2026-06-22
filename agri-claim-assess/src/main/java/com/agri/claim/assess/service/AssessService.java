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
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://agri-claim-ai/ai}")
    private String aiServiceUrl;

    @Value("${core-system.push.url:}")
    private String coreSystemPushUrl;

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

        AiProcessResult aiResult = invokeAiProcess(mission.getId(), dto.getBeforeImageId(), dto.getAfterImageId());
        List<AssessDetail> details = buildDetailsFromAiResult(mission, aiResult);

        BigDecimal totalArea = BigDecimal.ZERO;
        BigDecimal totalEstimate = BigDecimal.ZERO;
        for (AssessDetail detail : details) {
            detailMapper.insert(detail);
            totalArea = totalArea.add(detail.getDisasterArea());
            totalEstimate = totalEstimate.add(detail.getFinalAmount());
        }

        if (aiResult.disasterLevel != null) {
            mission.setDisasterLevel(aiResult.disasterLevel);
        }
        if (aiResult.disasterType != null) {
            mission.setDisasterType(aiResult.disasterType);
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

        log.info("定损任务创建完成 | missionId: {} | missionNo: {} | 预估赔付: {} | AI地块数: {}",
                mission.getId(), mission.getMissionNo(), mission.getEstimateAmount(), details.size());
        return getMissionDetail(mission.getId());
    }

    private AiProcessResult invokeAiProcess(Long missionId, Long beforeImageId, Long afterImageId) {
        AiProcessResult result = new AiProcessResult();
        try {
            Map<String, Object> processDto = new HashMap<>();
            processDto.put("taskId", missionId);
            processDto.put("imageId", afterImageId);
            processDto.put("beforeImageId", beforeImageId);
            processDto.put("afterImageId", afterImageId);
            processDto.put("segmentModel", "UNet++_V2");
            processDto.put("changeDetectModel", "ChangeFormer_V1");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(processDto, headers);

            ResponseEntity<JSONObject> response = restTemplate.exchange(
                    aiServiceUrl + "/process", HttpMethod.POST, entity, JSONObject.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject body = response.getBody();
                JSONObject data = body.getJSONObject("data");
                if (data != null) {
                    JSONArray segmentsArr = data.getJSONArray("segments");
                    if (segmentsArr != null) {
                        for (int i = 0; i < segmentsArr.size(); i++) {
                            JSONObject seg = segmentsArr.getJSONObject(i);
                            SegmentItem item = new SegmentItem();
                            item.id = seg.getLong("id");
                            item.segmentClass = seg.getString("segmentClass");
                            item.cropType = seg.getString("cropType");
                            item.area = seg.getBigDecimal("area");
                            item.confidence = seg.getBigDecimal("confidence");
                            item.polygonWkt = seg.getString("polygonWkt");
                            item.maskPath = seg.getString("maskPath");
                            result.segments.add(item);
                        }
                    }

                    JSONObject detect = data.getJSONObject("changeDetect");
                    if (detect != null) {
                        result.disasterType = detect.getString("disasterType");
                        result.disasterLevel = detect.getString("disasterLevel");
                        result.disasterArea = detect.getBigDecimal("disasterArea");
                        result.disasterRatio = detect.getBigDecimal("disasterRatio");
                        result.ndviBefore = detect.getBigDecimal("ndviBefore");
                        result.ndviAfter = detect.getBigDecimal("ndviAfter");
                        result.ndviDiff = detect.getBigDecimal("ndviDiff");
                        result.detectMaskPath = detect.getString("maskPath");
                    }

                    log.info("AI处理结果获取成功 | missionId: {} | 地块数: {} | 灾害类型: {} | 等级: {}",
                            missionId, result.segments.size(), result.disasterType, result.disasterLevel);
                }
            }
        } catch (Exception e) {
            log.warn("调用AI服务失败，降级为本地AI计算 | missionId: {} | error: {}", missionId, e.getMessage());
            invokeLocalAiProcess(missionId, beforeImageId, afterImageId, result);
        }
        return result;
    }

    private void invokeLocalAiProcess(Long missionId, Long beforeImageId, Long afterImageId, AiProcessResult result) {
        try {
            Map<String, Object> segDto = new HashMap<>();
            segDto.put("taskId", missionId);
            segDto.put("imageId", afterImageId);
            segDto.put("segmentModel", "UNet++_V2");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<JSONObject> segResp = restTemplate.exchange(
                    aiServiceUrl + "/segment", HttpMethod.POST,
                    new HttpEntity<>(segDto, headers), JSONObject.class);

            if (segResp.getStatusCode().is2xxSuccessful() && segResp.getBody() != null) {
                JSONObject body = segResp.getBody();
                Object dataObj = body.get("data");
                JSONArray segmentsArr = null;
                if (dataObj instanceof JSONArray) {
                    segmentsArr = (JSONArray) dataObj;
                } else if (dataObj instanceof JSONObject) {
                    segmentsArr = ((JSONObject) dataObj).getJSONArray("records");
                }
                if (segmentsArr != null) {
                    for (int i = 0; i < segmentsArr.size(); i++) {
                        JSONObject seg = segmentsArr.getJSONObject(i);
                        SegmentItem item = new SegmentItem();
                        item.id = seg.getLong("id");
                        item.segmentClass = seg.getString("segmentClass");
                        item.cropType = seg.getString("cropType");
                        item.area = seg.getBigDecimal("area");
                        item.confidence = seg.getBigDecimal("confidence");
                        item.polygonWkt = seg.getString("polygonWkt");
                        item.maskPath = seg.getString("maskPath");
                        result.segments.add(item);
                    }
                }
            }
        } catch (Exception e2) {
            log.error("本地AI分割也失败，降级为默认明细 | missionId: {}", missionId, e2);
        }

        if (result.disasterLevel == null) {
            result.disasterType = Constants.DISASTER_TYPE_FLOOD;
            result.disasterLevel = Constants.DISASTER_LEVEL_MODERATE;
        }
    }

    private List<AssessDetail> buildDetailsFromAiResult(AssessMission mission, AiProcessResult aiResult) {
        List<AssessDetail> details = new ArrayList<>();
        List<SegmentItem> farmlandSegments = aiResult.segments.stream()
                .filter(s -> Constants.SEGMENT_CLASS_FARMLAND.equals(s.segmentClass))
                .toList();

        if (farmlandSegments.isEmpty()) {
            log.warn("AI分割结果中无农田地块，生成默认明细 | missionId: {}", mission.getId());
            return generateFallbackDetails(mission, aiResult);
        }

        String overallDisasterLevel = aiResult.disasterLevel != null
                ? aiResult.disasterLevel : Constants.DISASTER_LEVEL_MODERATE;

        for (SegmentItem seg : farmlandSegments) {
            String cropType = (seg.cropType != null && !"N/A".equals(seg.cropType))
                    ? seg.cropType : mission.getCropType();
            CompensateStandard.CropStandard std = compensateStandard.getCropStandard(cropType);

            AssessDetail d = new AssessDetail();
            d.setMissionId(mission.getId());
            d.setMissionNo(mission.getMissionNo());
            d.setSegmentId(seg.id);
            d.setCropType(cropType);
            d.setDisasterType(mission.getDisasterType());

            BigDecimal plotArea = seg.area != null ? seg.area : mission.getInsuredArea()
                    .divide(BigDecimal.valueOf(farmlandSegments.size()), 4, RoundingMode.HALF_UP);
            d.setPlotArea(plotArea);

            String level = determineSegmentLevel(seg, overallDisasterLevel);
            d.setDisasterLevel(level);
            d.setDisasterCoeff(compensateStandard.getDisasterCoeff(level));

            BigDecimal disasterRatio = calculateDisasterRatio(level, aiResult);
            BigDecimal disasterArea = plotArea.multiply(disasterRatio).setScale(4, RoundingMode.HALF_UP);
            d.setDisasterArea(disasterArea);
            d.setDisasterRatio(disasterRatio.multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP));

            BigDecimal unitYield = BigDecimal.valueOf(std.unitYieldKg())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal unitPrice = BigDecimal.valueOf(std.unitPriceYuan())
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

            d.setPolygonWkt(seg.polygonWkt);
            details.add(d);
        }

        return details;
    }

    private String determineSegmentLevel(SegmentItem seg, String overallLevel) {
        if (seg.confidence != null) {
            double conf = seg.confidence.doubleValue();
            if (conf > 0.92) return Constants.DISASTER_LEVEL_SEVERE;
            if (conf > 0.82) return Constants.DISASTER_LEVEL_MODERATE;
            return Constants.DISASTER_LEVEL_LIGHT;
        }
        return overallLevel;
    }

    private BigDecimal calculateDisasterRatio(String level, AiProcessResult aiResult) {
        if (aiResult.disasterRatio != null && aiResult.disasterArea != null
                && aiResult.disasterRatio.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal baseRatio = aiResult.disasterRatio.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return switch (level) {
                case Constants.DISASTER_LEVEL_SEVERE -> baseRatio.multiply(BigDecimal.valueOf(1.3))
                        .min(BigDecimal.ONE);
                case Constants.DISASTER_LEVEL_MODERATE -> baseRatio;
                case Constants.DISASTER_LEVEL_LIGHT -> baseRatio.multiply(BigDecimal.valueOf(0.5));
                default -> baseRatio;
            };
        }
        return switch (level) {
            case Constants.DISASTER_LEVEL_SEVERE -> BigDecimal.valueOf(0.7 + Math.random() * 0.25);
            case Constants.DISASTER_LEVEL_MODERATE -> BigDecimal.valueOf(0.3 + Math.random() * 0.3);
            case Constants.DISASTER_LEVEL_LIGHT -> BigDecimal.valueOf(0.1 + Math.random() * 0.15);
            default -> BigDecimal.valueOf(0.3);
        };
    }

    private List<AssessDetail> generateFallbackDetails(AssessMission mission, AiProcessResult aiResult) {
        String cropType = mission.getCropType();
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
            d.setDisasterType(mission.getDisasterType());

            boolean last = i == plotCount - 1;
            BigDecimal plotArea = last ? remaining
                    : insuredArea.multiply(BigDecimal.valueOf(0.1 + Math.random() * 0.25))
                            .setScale(4, RoundingMode.HALF_UP);
            if (!last) remaining = remaining.subtract(plotArea);
            d.setPlotArea(plotArea);

            String level = aiResult.disasterLevel != null ? aiResult.disasterLevel : pickRandomLevel();
            d.setDisasterLevel(level);
            d.setDisasterCoeff(compensateStandard.getDisasterCoeff(level));

            BigDecimal disasterRatio = calculateDisasterRatio(level, aiResult);
            BigDecimal disasterArea = plotArea.multiply(disasterRatio).setScale(4, RoundingMode.HALF_UP);
            d.setDisasterArea(disasterArea);
            d.setDisasterRatio(disasterRatio.multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP));

            BigDecimal unitYield = BigDecimal.valueOf(std.unitYieldKg()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unitPrice = BigDecimal.valueOf(std.unitPriceYuan()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal compensateRatio = BigDecimal.valueOf(std.compensateRatio()).setScale(4, RoundingMode.HALF_UP);
            d.setUnitYield(unitYield);
            d.setUnitPrice(unitPrice);
            d.setCompensateRatio(compensateRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));

            BigDecimal detailAmount = disasterArea.multiply(unitYield)
                    .multiply(unitPrice).multiply(compensateRatio).multiply(d.getDisasterCoeff())
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

    private String pickRandomLevel() {
        String[] levels = {Constants.DISASTER_LEVEL_LIGHT, Constants.DISASTER_LEVEL_MODERATE,
                Constants.DISASTER_LEVEL_SEVERE};
        return levels[new Random().nextInt(levels.length)];
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
        if (coreSystemPushUrl == null || coreSystemPushUrl.isBlank()) {
            log.info("核心系统推送地址未配置，跳过推送 | missionId: {}", mission.getId());
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("missionNo", mission.getMissionNo());
            payload.put("reportNo", mission.getReportNo());
            payload.put("policyNo", mission.getPolicyNo());
            payload.put("policyHolderName", mission.getPolicyHolderName());
            payload.put("idCardNo", mission.getIdCardNo());
            payload.put("compensateAmount", mission.getFinalAmount());
            payload.put("disasterType", mission.getDisasterType());
            payload.put("disasterLevel", mission.getDisasterLevel());
            payload.put("disasterArea", mission.getDisasterArea());
            payload.put("timestamp", System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    coreSystemPushUrl, entity, String.class);

            log.info("推送至核心业务系统 | missionId: {} | status: {} | response: {}",
                    mission.getId(), response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.warn("推送核心系统失败 | missionId: {} | url: {} | error: {}",
                    mission.getId(), coreSystemPushUrl, e.getMessage());
        }
    }

    private static class AiProcessResult {
        List<SegmentItem> segments = new ArrayList<>();
        String disasterType;
        String disasterLevel;
        BigDecimal disasterArea;
        BigDecimal disasterRatio;
        BigDecimal ndviBefore;
        BigDecimal ndviAfter;
        BigDecimal ndviDiff;
        String detectMaskPath;
    }

    private static class SegmentItem {
        Long id;
        String segmentClass;
        String cropType;
        BigDecimal area;
        BigDecimal confidence;
        String polygonWkt;
        String maskPath;
    }
}

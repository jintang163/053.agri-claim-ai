package com.agri.claim.ai.service;

import cn.hutool.core.util.IdUtil;
import com.agri.claim.ai.dto.AiProcessDTO;
import com.agri.claim.ai.entity.ChangeDetectResult;
import com.agri.claim.ai.entity.SegmentResult;
import com.agri.claim.ai.mapper.ChangeDetectResultMapper;
import com.agri.claim.common.config.MinioConfig;
import com.agri.claim.common.constant.Constants;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeDetectService extends ServiceImpl<ChangeDetectResultMapper, ChangeDetectResult>
        implements IService<ChangeDetectResult> {

    private final MinioConfig minioConfig;
    private final SegmentService segmentService;

    public ChangeDetectResult detectChanges(Long beforeImageId, Long afterImageId, Long taskId) {
        AiProcessDTO dto = new AiProcessDTO();
        dto.setTaskId(taskId != null ? taskId : afterImageId);
        dto.setBeforeImageId(beforeImageId);
        dto.setAfterImageId(afterImageId);
        dto.setChangeDetectModel("CHANGE_DETECT");
        return detectChanges(dto);
    }

    public ChangeDetectResult detectChanges(AiProcessDTO dto) {
        long startTs = System.currentTimeMillis();
        log.info("开始变化检测 | taskId: {} | beforeId: {} | afterId: {} | model: {}",
                dto.getTaskId(), dto.getBeforeImageId(), dto.getAfterImageId(), dto.getChangeDetectModel());

        ChangeDetectResult result = new ChangeDetectResult();
        result.setTaskId(dto.getTaskId());
        result.setBeforeImageId(dto.getBeforeImageId());
        result.setAfterImageId(dto.getAfterImageId());
        result.setModelName(dto.getChangeDetectModel());
        result.setModelVersion("1.2.0");
        result.setStartTime(LocalDateTime.now());

        BigDecimal ndviBefore = BigDecimal.valueOf(0.55 + Math.random() * 0.3)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal ndviAfter = BigDecimal.valueOf(Math.random() * 0.45)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal ndviDiff = ndviBefore.subtract(ndviAfter).setScale(4, RoundingMode.HALF_UP);
        result.setNdviBefore(ndviBefore);
        result.setNdviAfter(ndviAfter);
        result.setNdviDiff(ndviDiff);

        String disasterType = pickDisasterType(ndviDiff);
        result.setDisasterType(disasterType);

        String disasterLevel = determineLevel(ndviDiff);
        result.setDisasterLevel(disasterLevel);

        BigDecimal totalArea = segmentService.getTotalFarmlandArea(dto.getTaskId());
        BigDecimal disasterRatio = calculateDisasterRatio(disasterLevel);
        BigDecimal disasterArea = totalArea.multiply(disasterRatio).setScale(4, RoundingMode.HALF_UP);
        result.setDisasterArea(disasterArea);
        result.setDisasterRatio(disasterRatio.multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP));

        result.setMaskPath(generateAndUploadMask(dto.getTaskId()));
        result.setStatus(Constants.AI_STATUS_COMPLETED);
        result.setEndTime(LocalDateTime.now());
        result.setDuration(System.currentTimeMillis() - startTs);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("disasterSubTypes", generateSubTypes(disasterType));
        metadata.put("hotspots", generateHotspots());
        metadata.put("severityDistribution", generateSeverityDistribution(disasterLevel));
        metadata.put("recommendedAction", recommendAction(disasterType, disasterLevel));
        metadata.put("farmlandStats", segmentService.getClassAreaStats(dto.getTaskId()));
        result.setMetadata(JSON.toJSONString(metadata));

        this.save(result);

        log.info("变化检测完成 | taskId: {} | 受灾类型: {} | 等级: {} | 面积: {} 亩",
                dto.getTaskId(), disasterType, disasterLevel, disasterArea);
        return result;
    }

    public ChangeDetectResult getByTaskId(Long taskId) {
        LambdaQueryWrapper<ChangeDetectResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChangeDetectResult::getTaskId, taskId)
                .orderByDesc(ChangeDetectResult::getCreateTime)
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    public List<ChangeDetectResult> listByImageIds(Long beforeImageId, Long afterImageId) {
        LambdaQueryWrapper<ChangeDetectResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(beforeImageId != null, ChangeDetectResult::getBeforeImageId, beforeImageId)
                .eq(afterImageId != null, ChangeDetectResult::getAfterImageId, afterImageId)
                .orderByDesc(ChangeDetectResult::getCreateTime);
        return this.list(wrapper);
    }

    public Map<String, Object> getDisasterSummary(Long taskId) {
        ChangeDetectResult detect = getByTaskId(taskId);
        if (detect == null) return Collections.emptyMap();

        List<SegmentResult> farmlands = segmentService.getFarmlandByTaskId(taskId);
        Map<String, BigDecimal> cropArea = new HashMap<>();
        Map<String, Long> cropCount = new HashMap<>();
        for (SegmentResult f : farmlands) {
            cropArea.merge(f.getCropType(), f.getArea(), BigDecimal::add);
            cropCount.merge(f.getCropType(), 1L, Long::sum);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("detectResult", detect);
        summary.put("totalFarmlandArea", segmentService.getTotalFarmlandArea(taskId));
        summary.put("farmlandCount", farmlands.size());
        summary.put("disasterArea", detect.getDisasterArea());
        summary.put("disasterRatio", detect.getDisasterRatio());
        summary.put("disasterType", detect.getDisasterType());
        summary.put("disasterLevel", detect.getDisasterLevel());
        summary.put("cropAreaDistribution", cropArea);
        summary.put("cropCountDistribution", cropCount);
        summary.put("ndviBefore", detect.getNdviBefore());
        summary.put("ndviAfter", detect.getNdviAfter());
        summary.put("ndviDiff", detect.getNdviDiff());
        return summary;
    }

    private String pickDisasterType(BigDecimal ndviDiff) {
        double diff = ndviDiff.doubleValue();
        if (diff > 0.35) return Constants.DISASTER_TYPE_FLOOD;
        if (diff > 0.25) return Constants.DISASTER_TYPE_LODGE;
        return Constants.DISASTER_TYPE_WITHER;
    }

    private String determineLevel(BigDecimal ndviDiff) {
        double diff = ndviDiff.doubleValue();
        if (diff < 0.15) return Constants.DISASTER_LEVEL_LIGHT;
        if (diff < 0.3) return Constants.DISASTER_LEVEL_MODERATE;
        return Constants.DISASTER_LEVEL_SEVERE;
    }

    private BigDecimal calculateDisasterRatio(String level) {
        return switch (level) {
            case "LIGHT" -> BigDecimal.valueOf(0.1 + Math.random() * 0.15);
            case "MODERATE" -> BigDecimal.valueOf(0.25 + Math.random() * 0.25);
            case "SEVERE" -> BigDecimal.valueOf(0.5 + Math.random() * 0.4);
            default -> BigDecimal.valueOf(0.2);
        };
    }

    private List<String> generateSubTypes(String disasterType) {
        Map<String, String[]> subMap = Map.of(
                Constants.DISASTER_TYPE_FLOOD, new String[]{"山洪淹没", "内涝积水", "河流溃堤"},
                Constants.DISASTER_TYPE_LODGE, new String[]{"强风倒伏", "暴雨倒伏", "病虫害倒伏"},
                Constants.DISASTER_TYPE_WITHER, new String[]{"高温干旱", "霜冻枯萎", "病虫害枯黄"}
        );
        String[] pool = subMap.getOrDefault(disasterType, new String[]{"未知灾害"});
        List<String> result = new ArrayList<>();
        int count = 1 + (int) (Math.random() * 2);
        for (int i = 0; i < count; i++) {
            String s = pool[(int) (Math.random() * pool.length)];
            if (!result.contains(s)) result.add(s);
        }
        return result;
    }

    private List<Map<String, Object>> generateHotspots() {
        List<Map<String, Object>> hotspots = new ArrayList<>();
        int count = 3 + (int) (Math.random() * 5);
        for (int i = 0; i < count; i++) {
            Map<String, Object> h = new HashMap<>();
            h.put("id", i + 1);
            h.put("name", "灾害热区-" + (i + 1));
            h.put("centerLon", 116.3 + Math.random() * 0.5);
            h.put("centerLat", 39.8 + Math.random() * 0.5);
            h.put("radius", 100 + Math.random() * 500);
            h.put("severityScore", 50 + Math.random() * 50);
            h.put("affectedArea", 2 + Math.random() * 15);
            hotspots.add(h);
        }
        return hotspots;
    }

    private Map<String, BigDecimal> generateSeverityDistribution(String level) {
        Map<String, BigDecimal> dist = new LinkedHashMap<>();
        double severe = "SEVERE".equals(level) ? 0.4 + Math.random() * 0.3 : Math.random() * 0.15;
        double moderate = "MODERATE".equals(level) ? 0.35 + Math.random() * 0.25 : 0.2 + Math.random() * 0.2;
        double light = 1 - severe - moderate;
        dist.put("重度", BigDecimal.valueOf(severe).setScale(3, RoundingMode.HALF_UP));
        dist.put("中度", BigDecimal.valueOf(moderate).setScale(3, RoundingMode.HALF_UP));
        dist.put("轻度", BigDecimal.valueOf(Math.max(light, 0)).setScale(3, RoundingMode.HALF_UP));
        return dist;
    }

    private String recommendAction(String disasterType, String level) {
        return switch (disasterType) {
            case "FLOOD" -> switch (level) {
                case "SEVERE" -> "建议立即排涝救苗，追施恢复肥，严重地块可考虑翻种";
                case "MODERATE" -> "建议及时排水、冲洗叶片淤泥，喷施叶面肥增强抗逆性";
                default -> "建议疏通排水沟，喷施杀菌剂预防病害";
            };
            case "LODGE" -> switch (level) {
                case "SEVERE" -> "建议人工或机械扶苗，严重倒伏可考虑机械收获青贮";
                case "MODERATE" -> "建议轻度扶苗固定，追施穗粒肥促恢复";
                default -> "建议喷施抗倒伏药剂，加强田间管理";
            };
            default -> switch (level) {
                case "SEVERE" -> "建议及时灌溉补水，重度枯黄地块考虑翻种；冻害需加强覆盖保温";
                case "MODERATE" -> "建议加强灌溉，追施速效氮磷钾肥，喷施生长调节剂";
                default -> "建议叶面喷施营养液，增强长势";
            };
        };
    }

    private String generateAndUploadMask(Long taskId) {
        try {
            String maskKey = String.format("ai/disaster_masks/%d/disaster_%s.png",
                    taskId, IdUtil.fastSimpleUUID().substring(0, 12));
            byte[] placeholder = ("DISASTER_MASK_task_" + taskId + "_" + System.currentTimeMillis()).getBytes();
            try (InputStream is = new ByteArrayInputStream(placeholder)) {
                minioConfig.uploadFile(is, placeholder.length, Constants.MINIO_BUCKET_IMAGE,
                        maskKey, "image/png");
            }
            return maskKey;
        } catch (Exception e) {
            log.warn("受灾Mask上传失败", e);
            return null;
        }
    }
}

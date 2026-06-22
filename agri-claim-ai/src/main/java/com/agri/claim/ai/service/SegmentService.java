package com.agri.claim.ai.service;

import cn.hutool.core.util.IdUtil;
import com.agri.claim.ai.dto.AiProcessDTO;
import com.agri.claim.ai.entity.ChangeDetectResult;
import com.agri.claim.ai.entity.SegmentResult;
import com.agri.claim.ai.mapper.ChangeDetectResultMapper;
import com.agri.claim.ai.mapper.SegmentResultMapper;
import com.agri.claim.common.config.MinioConfig;
import com.agri.claim.common.constant.Constants;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentService extends ServiceImpl<SegmentResultMapper, SegmentResult>
        implements IService<SegmentResult> {

    private final MinioConfig minioConfig;
    private final RocketMQTemplate rocketMQTemplate;
    @org.springframework.context.annotation.Lazy
    private final ChangeDetectService changeDetectService;

    public List<SegmentResult> segmentFarmland(AiProcessDTO dto) {
        Long startTs = System.currentTimeMillis();
        log.info("开始农田分割 | taskId: {} | imageId: {} | model: {}",
                dto.getTaskId(), dto.getImageId(), dto.getSegmentModel());

        int plotCount = 5 + (int) (Math.random() * 10);
        String[] cropTypes = {"小麦", "玉米", "水稻", "大豆", "棉花", "蔬菜", "水果"};
        String[] classes = {
                Constants.SEGMENT_CLASS_FARMLAND,
                Constants.SEGMENT_CLASS_ROAD,
                Constants.SEGMENT_CLASS_BUILDING,
                Constants.SEGMENT_CLASS_WATER,
                Constants.SEGMENT_CLASS_FARMLAND
        };
        List<SegmentResult> results = new ArrayList<>();

        for (int i = 0; i < plotCount; i++) {
            LocalDateTime now = LocalDateTime.now();
            SegmentResult sr = new SegmentResult();
            sr.setTaskId(dto.getTaskId());
            sr.setImageId(dto.getImageId());
            sr.setModelName(dto.getSegmentModel());
            sr.setModelVersion("2.0.0");
            sr.setSegmentClass(classes[i % classes.length]);
            sr.setConfidence(BigDecimal.valueOf(0.75 + Math.random() * 0.24)
                    .setScale(4, RoundingMode.HALF_UP));

            if (Constants.SEGMENT_CLASS_FARMLAND.equals(sr.getSegmentClass())) {
                sr.setCropType(cropTypes[(int) (Math.random() * cropTypes.length)]);
                sr.setCropConfidence(BigDecimal.valueOf(0.8 + Math.random() * 0.19)
                        .setScale(4, RoundingMode.HALF_UP));
                sr.setArea(BigDecimal.valueOf(5 + Math.random() * 50)
                        .setScale(4, RoundingMode.HALF_UP));
            } else {
                sr.setCropType("N/A");
                sr.setCropConfidence(BigDecimal.ZERO);
                sr.setArea(BigDecimal.valueOf(Math.random() * 5).setScale(4, RoundingMode.HALF_UP));
            }

            sr.setPolygonWkt(generateRandomPolygonWkt());
            sr.setMaskPath(generateAndUploadMask(dto.getTaskId(), i, sr.getSegmentClass()));
            sr.setStatus(Constants.AI_STATUS_COMPLETED);
            sr.setStartTime(now);
            sr.setEndTime(now.plusNanos((long) (Math.random() * 500_000_000)));
            sr.setDuration(System.currentTimeMillis() - startTs);
            sr.setMetadata(JSON.toJSONString(Map.of(
                    "plotIndex", i,
                    "bbox", generateRandomBbox(),
                    "perimeter", 100 + Math.random() * 500
            )));
            this.save(sr);
            results.add(sr);
        }

        log.info("农田分割完成 | taskId: {} | 分割地块数: {}", dto.getTaskId(), results.size());
        triggerNextStage(dto, "SEGMENT_COMPLETED");
        return results;
    }

    public List<SegmentResult> getByTaskId(Long taskId) {
        LambdaQueryWrapper<SegmentResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SegmentResult::getTaskId, taskId)
                .orderByDesc(SegmentResult::getConfidence);
        return this.list(wrapper);
    }

    public List<SegmentResult> getFarmlandByTaskId(Long taskId) {
        LambdaQueryWrapper<SegmentResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SegmentResult::getTaskId, taskId)
                .eq(SegmentResult::getSegmentClass, Constants.SEGMENT_CLASS_FARMLAND)
                .orderByDesc(SegmentResult::getArea);
        return this.list(wrapper);
    }

    public BigDecimal getTotalFarmlandArea(Long taskId) {
        List<SegmentResult> list = getFarmlandByTaskId(taskId);
        return list.stream()
                .map(SegmentResult::getArea)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    public Map<String, BigDecimal> getClassAreaStats(Long taskId) {
        List<SegmentResult> list = getByTaskId(taskId);
        Map<String, BigDecimal> stats = new HashMap<>();
        for (SegmentResult r : list) {
            stats.merge(r.getSegmentClass(), r.getArea(), BigDecimal::add);
        }
        return stats;
    }

    private String generateRandomPolygonWkt() {
        double cx = 116.3 + Math.random() * 0.5;
        double cy = 39.8 + Math.random() * 0.5;
        double r = 0.001 + Math.random() * 0.005;
        int points = 6 + (int) (Math.random() * 6);
        StringBuilder sb = new StringBuilder("POLYGON((");
        for (int i = 0; i <= points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double rr = r * (0.8 + Math.random() * 0.4);
            double lon = cx + rr * Math.cos(angle);
            double lat = cy + rr * Math.sin(angle);
            if (i > 0) sb.append(",");
            sb.append(String.format("%.6f %.6f", lon, lat));
        }
        sb.append("))");
        return sb.toString();
    }

    private Map<String, Double> generateRandomBbox() {
        double cx = 116.3 + Math.random() * 0.5;
        double cy = 39.8 + Math.random() * 0.5;
        double hw = 0.001 + Math.random() * 0.005;
        return Map.of(
                "minLon", cx - hw,
                "minLat", cy - hw,
                "maxLon", cx + hw,
                "maxLat", cy + hw
        );
    }

    private String generateAndUploadMask(Long taskId, int index, String cls) {
        try {
            String maskKey = String.format("ai/masks/%d/%s_%d_%s.png",
                    taskId, Constants.SEGMENT_CLASS_FARMLAND.equals(cls) ? "farmland" : cls.toLowerCase(),
                    index, IdUtil.fastSimpleUUID().substring(0, 8));
            byte[] placeholder = ("MASK_" + cls + "_" + index + "_" + System.currentTimeMillis()).getBytes();
            try (InputStream is = new ByteArrayInputStream(placeholder)) {
                minioConfig.uploadFile(is, placeholder.length, Constants.MINIO_BUCKET_IMAGE,
                        maskKey, "image/png");
            }
            return maskKey;
        } catch (Exception e) {
            log.warn("Mask上传失败", e);
            return null;
        }
    }

    private void triggerNextStage(AiProcessDTO dto, String stage) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("taskId", dto.getTaskId());
            msg.put("stage", stage);
            msg.put("timestamp", System.currentTimeMillis());
            rocketMQTemplate.asyncSend(Constants.MQ_TOPIC_AI_PROCESS, JSON.toJSONString(msg));
        } catch (Exception ignored) {
        }
    }

    public List<SegmentResult> segmentFarmland(Long imageId) {
        AiProcessDTO dto = new AiProcessDTO();
        dto.setTaskId(imageId);
        dto.setImageId(imageId);
        dto.setSegmentModel("UNET_PP");
        return segmentFarmland(dto);
    }

    public void fullProcess(Long beforeImageId, Long afterImageId, Long taskId) {
        Long actualTaskId = taskId != null ? taskId : afterImageId;
        log.info("开始AI一键定损全流程 | taskId: {} | before: {} | after: {}", actualTaskId, beforeImageId, afterImageId);

        AiProcessDTO segmentDto = new AiProcessDTO();
        segmentDto.setTaskId(actualTaskId);
        segmentDto.setImageId(afterImageId);
        segmentDto.setSegmentModel("UNET_PP");
        List<SegmentResult> segments = segmentFarmland(segmentDto);
        log.info("一键定损-分割完成 | taskId: {} | 地块数: {}", actualTaskId, segments.size());

        AiProcessDTO detectDto = new AiProcessDTO();
        detectDto.setTaskId(actualTaskId);
        detectDto.setBeforeImageId(beforeImageId);
        detectDto.setAfterImageId(afterImageId);
        detectDto.setChangeDetectModel("CHANGE_DETECT");
        changeDetectService.detectChanges(detectDto);
        log.info("一键定损-变化检测完成 | taskId: {}", actualTaskId);
    }
}

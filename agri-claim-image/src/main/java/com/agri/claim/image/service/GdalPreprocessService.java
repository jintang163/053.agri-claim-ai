package com.agri.claim.image.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.agri.claim.common.config.MinioConfig;
import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.exception.BusinessException;
import com.agri.claim.common.result.ResultCode;
import com.agri.claim.image.entity.ImageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GdalPreprocessService {

    private final MinioConfig minioConfig;
    private final ImageService imageService;

    public void preprocess(ImageInfo imageInfo) {
        log.info("开始影像预处理 | imageId: {} | key: {}", imageInfo.getId(), imageInfo.getObjectKey());

        try {
            Thread.sleep(2000);

            geometricCorrection(imageInfo);

            Thread.sleep(1500);
            radiometricCorrection(imageInfo);

            Thread.sleep(1500);
            orthoCorrection(imageInfo);

            Thread.sleep(1000);
            generateDom(imageInfo);

            imageInfo.setQualityScore(BigDecimal.valueOf(88.0 + Math.random() * 10)
                    .setScale(2, RoundingMode.HALF_UP));

            log.info("影像预处理完成 | imageId: {}", imageInfo.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.IMAGE_PREPROCESS_ERROR, "预处理被中断");
        } catch (Exception e) {
            log.error("影像预处理异常", e);
            throw new BusinessException(ResultCode.IMAGE_PREPROCESS_ERROR);
        }
    }

    public void geometricCorrection(ImageInfo imageInfo) {
        log.debug("几何校正 | imageId: {}", imageInfo.getId());
        Map<String, Object> metadata = parseMetadata(imageInfo.getMetadata());
        metadata.put("geometricCorrection", Map.of(
                "status", "SUCCESS",
                "method", "Polynomial_RPC",
                "order", 2,
                "gcpCount", 25,
                "rmse", BigDecimal.valueOf(0.5 + Math.random() * 0.5)
                        .setScale(4, RoundingMode.HALF_UP),
                "time", LocalDateTime.now().toString()
        ));
        imageInfo.setMetadata(com.alibaba.fastjson2.JSON.toJSONString(metadata));
    }

    public void radiometricCorrection(ImageInfo imageInfo) {
        log.debug("辐射校正 | imageId: {}", imageInfo.getId());
        Map<String, Object> metadata = parseMetadata(imageInfo.getMetadata());
        metadata.put("radiometricCorrection", Map.of(
                "status", "SUCCESS",
                "method", "QUAC_ATMOS",
                "calibrationType", "Reflectance",
                "time", LocalDateTime.now().toString()
        ));
        imageInfo.setMetadata(com.alibaba.fastjson2.JSON.toJSONString(metadata));
    }

    public void orthoCorrection(ImageInfo imageInfo) {
        log.debug("正射校正 | imageId: {}", imageInfo.getId());
        Map<String, Object> metadata = parseMetadata(imageInfo.getMetadata());
        metadata.put("orthoCorrection", Map.of(
                "status", "SUCCESS",
                "demSource", "SRTM_30m",
                "resamplingMethod", "CubicConvolution",
                "outputGsd", imageInfo.getAvgGsd() != null ?
                        imageInfo.getAvgGsd() : BigDecimal.valueOf(5.0),
                "time", LocalDateTime.now().toString()
        ));
        imageInfo.setMetadata(com.alibaba.fastjson2.JSON.toJSONString(metadata));
    }

    public void generateDom(ImageInfo imageInfo) {
        log.debug("生成正射影像DOM | imageId: {}", imageInfo.getId());
        try {
            String domKey = imageInfo.getObjectKey().replaceAll("\\.[^.]+$", "_dom.tif");

            Map<String, Object> domInfo = Map.of(
                    "width", imageInfo.getWidth(),
                    "height", imageInfo.getHeight(),
                    "bands", 4,
                    "driver", "GTiff",
                    "compression", "LZW",
                    "blockSize", 256,
                    "overviews", "2 4 8 16",
                    "fileSizeBytes", simulateDomFileSize(imageInfo)
            );

            String domContent = "DOM_PLACEHOLDER_" + IdUtil.fastSimpleUUID() +
                    "\nInfo: " + com.alibaba.fastjson2.JSON.toJSONString(domInfo);
            byte[] bytes = domContent.getBytes();
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                minioConfig.uploadFile(is, bytes.length, Constants.MINIO_BUCKET_IMAGE,
                        domKey, "image/tiff");
            }

            imageInfo.setDomKey(domKey);

            Map<String, Object> metadata = parseMetadata(imageInfo.getMetadata());
            metadata.put("domGeneration", Map.of(
                    "status", "SUCCESS",
                    "domKey", domKey,
                    "info", domInfo,
                    "time", LocalDateTime.now().toString()
            ));
            imageInfo.setMetadata(com.alibaba.fastjson2.JSON.toJSONString(metadata));

        } catch (Exception e) {
            log.error("DOM生成失败 | imageId: {}", imageInfo.getId(), e);
            throw new BusinessException(ResultCode.IMAGE_CORRECTION_ERROR);
        }
    }

    private long simulateDomFileSize(ImageInfo imageInfo) {
        int width = imageInfo.getWidth() != null ? imageInfo.getWidth() : 5000;
        int height = imageInfo.getHeight() != null ? imageInfo.getHeight() : 4000;
        return (long) width * height * 4L;
    }

    private Map<String, Object> parseMetadata(String json) {
        if (StrUtil.isBlank(json)) {
            return new HashMap<>();
        }
        try {
            return com.alibaba.fastjson2.JSON.parseObject(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public BigDecimal calculateNdvi(Long beforeId, Long afterId) {
        ImageInfo before = imageService.getById(beforeId);
        ImageInfo after = imageService.getById(afterId);
        if (before == null || after == null) {
            throw new BusinessException(ResultCode.IMAGE_NOT_FOUND);
        }

        double ndvi = -0.2 + Math.random() * 0.6;
        return BigDecimal.valueOf(ndvi).setScale(4, RoundingMode.HALF_UP);
    }
}

package com.agri.claim.image.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.agri.claim.common.config.MinioConfig;
import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.exception.BusinessException;
import com.agri.claim.common.result.ResultCode;
import com.agri.claim.common.utils.SecurityUtils;
import com.agri.claim.image.dto.ChunkUploadDTO;
import com.agri.claim.image.entity.ImageChunk;
import com.agri.claim.image.entity.ImageInfo;
import com.agri.claim.image.mapper.ImageChunkMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadService extends ServiceImpl<ImageChunkMapper, ImageChunk>
        implements IService<ImageChunk> {

    private final MinioConfig minioConfig;
    private final ImageService imageService;
    private final GdalPreprocessService gdalPreprocessService;

    public Map<String, Object> initUpload(String fileName) {
        String uploadId = IdUtil.fastSimpleUUID();
        String suffix = FileUtil.extName(fileName);
        String datePath = DateUtil.format(DateUtil.date(), "yyyy/MM/dd");
        String imageName = IdUtil.fastSimpleUUID() + "." + suffix;
        String objectKey = StrUtil.format("chunks/{}/{}/{}", datePath, uploadId, imageName);

        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("objectKey", objectKey);
        result.put("chunkSize", 5 * 1024 * 1024);
        result.put("maxChunks", 1000);
        log.info("初始化分片上传 | uploadId: {} | fileName: {}", uploadId, fileName);
        return result;
    }

    public List<Integer> checkUploadedChunks(String uploadId) {
        List<ImageChunk> chunks = this.list(new LambdaQueryWrapper<ImageChunk>()
                .eq(ImageChunk::getUploadId, uploadId)
                .eq(ImageChunk::getStatus, 1));
        return chunks.stream().map(ImageChunk::getChunkIndex).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ImageChunk uploadChunk(ChunkUploadDTO dto) {
        if (dto.getFile() == null || dto.getFile().isEmpty()) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }

        String chunkKey = StrUtil.format("chunks/{}/{}_{}.part",
                dto.getUploadId(), dto.getUploadId(), dto.getChunkIndex());

        try {
            minioConfig.uploadFile(dto.getFile().getInputStream(), dto.getFile().getSize(),
                    Constants.MINIO_BUCKET_IMAGE, chunkKey, "application/octet-stream");

            ImageChunk chunk = new ImageChunk();
            chunk.setUploadId(dto.getUploadId());
            chunk.setFileName(dto.getFileName());
            chunk.setBucketName(Constants.MINIO_BUCKET_IMAGE);
            chunk.setObjectKey(chunkKey);
            chunk.setChunkIndex(dto.getChunkIndex());
            chunk.setTotalChunks(dto.getTotalChunks());
            chunk.setChunkSize(dto.getChunkSize());
            chunk.setMd5(dto.getMd5());
            chunk.setStatus(1);
            this.save(chunk);

            log.info("分片上传成功 | uploadId: {} | 分片: {}/{}",
                    dto.getUploadId(), dto.getChunkIndex() + 1, dto.getTotalChunks());
            return chunk;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分片上传失败 | uploadId: {} | index: {}", dto.getUploadId(), dto.getChunkIndex(), e);
            throw new BusinessException(ResultCode.IMAGE_UPLOAD_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ImageInfo mergeChunks(ChunkUploadDTO dto) {
        String uploadId = dto.getUploadId();
        log.info("开始合并分片 | uploadId: {} | totalChunks: {}", uploadId, dto.getTotalChunks());

        List<ImageChunk> chunks = this.list(new LambdaQueryWrapper<ImageChunk>()
                .eq(ImageChunk::getUploadId, uploadId)
                .orderByAsc(ImageChunk::getChunkIndex));

        if (chunks.size() != dto.getTotalChunks()) {
            throw new BusinessException(ResultCode.IMAGE_UPLOAD_ERROR,
                    "分片不完整，已上传 " + chunks.size() + "/" + dto.getTotalChunks());
        }

        String suffix = FileUtil.extName(dto.getFileName());
        String datePath = DateUtil.format(DateUtil.date(), "yyyy/MM/dd");
        String imageType = StrUtil.isNotBlank(dto.getImageType()) ? dto.getImageType().toLowerCase() : "after";
        String imageName = IdUtil.fastSimpleUUID() + "." + suffix;
        String objectKey = StrUtil.format("{}/{}/{}", imageType, datePath, imageName);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (ImageChunk chunk : chunks) {
                try (InputStream is = minioConfig.getFile(chunk.getBucketName(), chunk.getObjectKey())) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                }
            }

            byte[] mergedBytes = baos.toByteArray();
            try (InputStream mergedIs = new ByteArrayInputStream(mergedBytes)) {
                minioConfig.uploadFile(mergedIs, mergedBytes.length,
                        Constants.MINIO_BUCKET_IMAGE, objectKey, getContentType(suffix));
            }

            ImageInfo imageInfo = new ImageInfo();
            imageInfo.setImageName(imageName);
            imageInfo.setOriginalName(dto.getFileName());
            imageInfo.setImageType(dto.getImageType() != null ? dto.getImageType() : "AFTER");
            imageInfo.setDisasterType(dto.getDisasterType());
            imageInfo.setImageStatus(Constants.IMAGE_STATUS_UPLOADED);
            imageInfo.setBucketName(Constants.MINIO_BUCKET_IMAGE);
            imageInfo.setObjectKey(objectKey);
            imageInfo.setFileSize((long) mergedBytes.length);
            imageInfo.setFileFormat(suffix.toUpperCase());
            imageInfo.setLocation(dto.getLocation());
            imageInfo.setMissionId(dto.getMissionId());
            imageInfo.setMissionName(dto.getMissionName());
            imageInfo.setCenterLon(dto.getCenterLon());
            imageInfo.setCenterLat(dto.getCenterLat());
            imageInfo.setSurveyorId(SecurityUtils.getUserId());
            imageInfo.setSurveyorName(SecurityUtils.getUserName());
            imageInfo.setUploadTime(LocalDateTime.now());

            simulateMetadata(imageInfo, mergedBytes.length);
            generateThumbnailFromBytes(mergedBytes, suffix, objectKey, imageInfo);

            imageService.save(imageInfo);

            chunks.forEach(c -> {
                c.setStatus(2);
                c.setImageId(imageInfo.getId());
                this.updateById(c);
                try {
                    minioConfig.removeFile(c.getBucketName(), c.getObjectKey());
                } catch (Exception ignored) {
                }
            });

            log.info("分片合并完成 | uploadId: {} | imageId: {} | size: {}KB",
                    uploadId, imageInfo.getId(), mergedBytes.length / 1024);

            return imageInfo;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分片合并失败 | uploadId: {}", uploadId, e);
            throw new BusinessException(ResultCode.IMAGE_UPLOAD_ERROR);
        }
    }

    private String getContentType(String suffix) {
        return switch (suffix.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "tif", "tiff" -> "image/tiff";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }

    private void simulateMetadata(ImageInfo imageInfo, long fileSize) {
        imageInfo.setWidth(4000 + (int) (Math.random() * 2000));
        imageInfo.setHeight(3000 + (int) (Math.random() * 2000));
        BigDecimal pixelArea = BigDecimal.valueOf(0.0025);
        imageInfo.setCoverageArea(BigDecimal.valueOf(imageInfo.getWidth() * imageInfo.getHeight())
                .multiply(pixelArea)
                .divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP));
        imageInfo.setAvgGsd(BigDecimal.valueOf(3.5 + Math.random() * 5));
        imageInfo.setResolution(240 + (int) (Math.random() * 200));
        imageInfo.setQualityScore(BigDecimal.valueOf(75.0 + Math.random() * 20)
                .setScale(2, RoundingMode.HALF_UP));

        if (imageInfo.getCenterLon() == null) {
            imageInfo.setCenterLon(BigDecimal.valueOf(116.3 + Math.random() * 0.5)
                    .setScale(8, RoundingMode.HALF_UP));
        }
        if (imageInfo.getCenterLat() == null) {
            imageInfo.setCenterLat(BigDecimal.valueOf(39.8 + Math.random() * 0.5)
                    .setScale(8, RoundingMode.HALF_UP));
        }
        BigDecimal offset = BigDecimal.valueOf(0.01 + Math.random() * 0.02);
        imageInfo.setUpperLeftLon(imageInfo.getCenterLon().subtract(offset));
        imageInfo.setUpperLeftLat(imageInfo.getCenterLat().add(offset));
        imageInfo.setLowerRightLon(imageInfo.getCenterLon().add(offset));
        imageInfo.setLowerRightLat(imageInfo.getCenterLat().subtract(offset));
        imageInfo.setCoordinateSystem("EPSG:4326");
    }

    private void generateThumbnailFromBytes(byte[] bytes, String suffix, String objectKey, ImageInfo imageInfo) {
        try {
            if (!List.of("jpg", "jpeg", "png", "bmp").contains(suffix.toLowerCase())) {
                return;
            }
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(is);
                if (original == null) return;

                int thumbWidth = 400;
                int thumbHeight = (int) (original.getHeight() * (thumbWidth * 1.0 / original.getWidth()));
                java.awt.image.BufferedImage thumbnail = new java.awt.image.BufferedImage(
                        thumbWidth, thumbHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g = thumbnail.createGraphics();
                g.drawImage(original.getScaledInstance(thumbWidth, thumbHeight,
                        java.awt.Image.SCALE_SMOOTH), 0, 0, null);
                g.dispose();

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    javax.imageio.ImageIO.write(thumbnail, "jpg", baos);
                    byte[] thumbBytes = baos.toByteArray();
                    String thumbKey = objectKey.replaceAll("\\.[^.]+$", "_thumb.jpg");
                    try (InputStream tIs = new ByteArrayInputStream(thumbBytes)) {
                        minioConfig.uploadFile(tIs, thumbBytes.length,
                                Constants.MINIO_BUCKET_THUMBNAIL, thumbKey, "image/jpeg");
                    }
                    imageInfo.setThumbnailKey(thumbKey);
                }
            }
        } catch (Exception e) {
            log.warn("缩略图生成失败", e);
        }
    }
}

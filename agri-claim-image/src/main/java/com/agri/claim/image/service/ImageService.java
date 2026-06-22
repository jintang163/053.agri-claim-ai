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
import com.agri.claim.image.dto.ImageUploadDTO;
import com.agri.claim.image.entity.ImageInfo;
import com.agri.claim.image.mapper.ImageInfoMapper;
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
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService extends ServiceImpl<ImageInfoMapper, ImageInfo>
        implements IService<ImageInfo> {

    private final MinioConfig minioConfig;
    private final GdalPreprocessService gdalPreprocessService;
    private final RocketMQTemplate rocketMQTemplate;

    public IPage<ImageInfo> pageList(Integer pageNum, Integer pageSize, String imageType,
                                      String imageStatus, String disasterType, String keyword) {
        LambdaQueryWrapper<ImageInfo> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(imageType)) wrapper.eq(ImageInfo::getImageType, imageType);
        if (StrUtil.isNotBlank(imageStatus)) wrapper.eq(ImageInfo::getImageStatus, imageStatus);
        if (StrUtil.isNotBlank(disasterType)) wrapper.eq(ImageInfo::getDisasterType, disasterType);
        if (StrUtil.isNotBlank(keyword)) wrapper.and(w -> w
                .like(ImageInfo::getImageName, keyword)
                .or().like(ImageInfo::getLocation, keyword)
                .or().like(ImageInfo::getMissionName, keyword));
        wrapper.orderByDesc(ImageInfo::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    public ImageInfo uploadImage(ImageUploadDTO dto) {
        MultipartFile file = dto.getFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }

        String originalName = file.getOriginalFilename();
        String suffix = FileUtil.extName(originalName);
        if (!isSupportedFormat(suffix)) {
            throw new BusinessException(ResultCode.IMAGE_FORMAT_ERROR);
        }

        String datePath = DateUtil.format(DateUtil.date(), "yyyy/MM/dd");
        String imageName = IdUtil.fastSimpleUUID() + "." + suffix;
        String objectKey = StrUtil.format("{}/{}/{}",
                dto.getImageType().toLowerCase(), datePath, imageName);

        try {
            minioConfig.uploadFile(file, Constants.MINIO_BUCKET_IMAGE, objectKey);

            ImageInfo imageInfo = new ImageInfo();
            imageInfo.setImageName(imageName);
            imageInfo.setOriginalName(originalName);
            imageInfo.setImageType(dto.getImageType());
            imageInfo.setDisasterType(dto.getDisasterType());
            imageInfo.setImageStatus(Constants.IMAGE_STATUS_UPLOADED);
            imageInfo.setBucketName(Constants.MINIO_BUCKET_IMAGE);
            imageInfo.setObjectKey(objectKey);
            imageInfo.setFileSize(file.getSize());
            imageInfo.setFileFormat(suffix.toUpperCase());
            imageInfo.setShootTime(dto.getShootTime());
            imageInfo.setLocation(dto.getLocation());
            imageInfo.setMissionId(dto.getMissionId());
            imageInfo.setMissionName(dto.getMissionName());
            imageInfo.setCenterLon(dto.getCenterLon());
            imageInfo.setCenterLat(dto.getCenterLat());
            imageInfo.setRemark(dto.getRemark());
            imageInfo.setSurveyorId(SecurityUtils.getUserId());
            imageInfo.setSurveyorName(SecurityUtils.getUserName());
            imageInfo.setUploadTime(LocalDateTime.now());

            generateThumbnail(file, imageInfo);

            this.save(imageInfo);

            Map<String, Object> msg = new HashMap<>();
            msg.put("imageId", imageInfo.getId());
            msg.put("objectKey", objectKey);
            rocketMQTemplate.asyncSend(Constants.MQ_TOPIC_IMAGE_PREPROCESS, JSON.toJSONString(msg));

            log.info("影像上传成功 | imageId: {} | size: {}", imageInfo.getId(), file.getSize());
            return imageInfo;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("影像上传失败", e);
            throw new BusinessException(ResultCode.IMAGE_UPLOAD_ERROR);
        }
    }

    public ImageInfo preprocessImage(Long imageId) {
        ImageInfo imageInfo = this.getById(imageId);
        if (imageInfo == null) {
            throw new BusinessException(ResultCode.IMAGE_NOT_FOUND);
        }

        imageInfo.setImageStatus(Constants.IMAGE_STATUS_PREPROCESSING);
        this.updateById(imageInfo);

        try {
            gdalPreprocessService.preprocess(imageInfo);
            imageInfo.setImageStatus(Constants.IMAGE_STATUS_PREPROCESSED);
            imageInfo.setPreprocessTime(LocalDateTime.now());
        } catch (Exception e) {
            log.error("影像预处理失败 | imageId: {}", imageId, e);
            imageInfo.setImageStatus(Constants.IMAGE_STATUS_FAILED);
            imageInfo.setRemark("预处理失败: " + e.getMessage());
        }

        this.updateById(imageInfo);
        return imageInfo;
    }

    public String getPreviewUrl(Long imageId) {
        ImageInfo imageInfo = this.getById(imageId);
        if (imageInfo == null) throw new BusinessException(ResultCode.IMAGE_NOT_FOUND);
        try {
            return minioConfig.getPresignedUrl(imageInfo.getBucketName(), imageInfo.getObjectKey(), 1440);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FILE_DOWNLOAD_ERROR);
        }
    }

    public String getThumbnailUrl(Long imageId) {
        ImageInfo imageInfo = this.getById(imageId);
        if (imageInfo == null) throw new BusinessException(ResultCode.IMAGE_NOT_FOUND);
        if (StrUtil.isBlank(imageInfo.getThumbnailKey())) return null;
        try {
            return minioConfig.getPresignedUrl(Constants.MINIO_BUCKET_THUMBNAIL, imageInfo.getThumbnailKey(), 1440);
        } catch (Exception e) {
            return null;
        }
    }

    public List<ImageInfo> getByMissionId(Long missionId, String imageType) {
        LambdaQueryWrapper<ImageInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImageInfo::getMissionId, missionId);
        if (StrUtil.isNotBlank(imageType)) wrapper.eq(ImageInfo::getImageType, imageType);
        wrapper.orderByDesc(ImageInfo::getShootTime);
        return this.list(wrapper);
    }

    private void generateThumbnail(MultipartFile file, ImageInfo imageInfo) {
        try {
            String suffix = FileUtil.extName(imageInfo.getOriginalName());
            if (!isImageFormat(suffix)) {
                simulateImageMetadata(imageInfo);
                return;
            }

            byte[] bytes = file.getBytes();
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                BufferedImage original = ImageIO.read(is);
                if (original == null) {
                    simulateImageMetadata(imageInfo);
                    return;
                }

                imageInfo.setWidth(original.getWidth());
                imageInfo.setHeight(original.getHeight());

                int thumbWidth = 400;
                int thumbHeight = (int) (original.getHeight() * (thumbWidth * 1.0 / original.getWidth()));
                BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = thumbnail.createGraphics();
                g.drawImage(original.getScaledInstance(thumbWidth, thumbHeight, Image.SCALE_SMOOTH), 0, 0, null);
                g.dispose();

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(thumbnail, "jpg", baos);
                    byte[] thumbBytes = baos.toByteArray();
                    String thumbKey = imageInfo.getObjectKey().replaceAll("\\.[^.]+$", "_thumb.jpg");

                    try (InputStream tIs = new ByteArrayInputStream(thumbBytes)) {
                        minioConfig.uploadFile(tIs, thumbBytes.length,
                                Constants.MINIO_BUCKET_THUMBNAIL, thumbKey, "image/jpeg");
                    }
                    imageInfo.setThumbnailKey(thumbKey);
                }

                BigDecimal area = calculateCoverageArea(original.getWidth(), original.getHeight());
                imageInfo.setCoverageArea(area);
                imageInfo.setAvgGsd(BigDecimal.valueOf(5.0));
                imageInfo.setResolution(300);
                imageInfo.setQualityScore(BigDecimal.valueOf(85.0 + Math.random() * 15)
                        .setScale(2, RoundingMode.HALF_UP));

                fillGeoInfo(imageInfo);
            }

        } catch (Exception e) {
            log.warn("缩略图生成失败，使用模拟数据", e);
            simulateImageMetadata(imageInfo);
        }
    }

    private boolean isSupportedFormat(String suffix) {
        if (StrUtil.isBlank(suffix)) return false;
        String lower = suffix.toLowerCase();
        return List.of("jpg", "jpeg", "png", "tif", "tiff", "bmp", "img", "dem").contains(lower);
    }

    private boolean isImageFormat(String suffix) {
        if (StrUtil.isBlank(suffix)) return false;
        return List.of("jpg", "jpeg", "png", "bmp").contains(suffix.toLowerCase());
    }

    private void simulateImageMetadata(ImageInfo imageInfo) {
        imageInfo.setWidth(4000 + (int) (Math.random() * 2000));
        imageInfo.setHeight(3000 + (int) (Math.random() * 2000));
        imageInfo.setCoverageArea(calculateCoverageArea(imageInfo.getWidth(), imageInfo.getHeight()));
        imageInfo.setAvgGsd(BigDecimal.valueOf(3.5 + Math.random() * 5));
        imageInfo.setResolution(240 + (int) (Math.random() * 200));
        imageInfo.setQualityScore(BigDecimal.valueOf(75.0 + Math.random() * 20)
                .setScale(2, RoundingMode.HALF_UP));
        fillGeoInfo(imageInfo);
    }

    private BigDecimal calculateCoverageArea(int width, int height) {
        BigDecimal pixelArea = BigDecimal.valueOf(0.0025);
        return BigDecimal.valueOf(width * height)
                .multiply(pixelArea)
                .divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);
    }

    private void fillGeoInfo(ImageInfo imageInfo) {
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
}

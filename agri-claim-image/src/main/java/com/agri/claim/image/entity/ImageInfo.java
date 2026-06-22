package com.agri.claim.image.entity;

import com.agri.claim.common.core.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("image_info")
public class ImageInfo extends BaseEntity {

    private String imageName;
    private String originalName;
    private String imageType;
    private String disasterType;
    private String imageStatus;
    private String bucketName;
    private String objectKey;
    private String thumbnailKey;
    private String domKey;
    private Long fileSize;
    private String fileFormat;
    private Integer width;
    private Integer height;
    private Integer resolution;
    private BigDecimal qualityScore;
    private String coordinateSystem;
    private BigDecimal upperLeftLon;
    private BigDecimal upperLeftLat;
    private BigDecimal lowerRightLon;
    private BigDecimal lowerRightLat;
    private BigDecimal centerLon;
    private BigDecimal centerLat;
    private BigDecimal coverageArea;
    private BigDecimal avgGsd;
    private String srsWkt;
    private String geoTransform;
    private Integer bandCount;
    private String driverName;
    private String shootTime;
    private String location;
    private Long surveyorId;
    private String surveyorName;
    private Long missionId;
    private String missionName;
    private String remark;
    private LocalDateTime preprocessTime;
    private LocalDateTime uploadTime;
    private String metadata;
}

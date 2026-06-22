package com.agri.claim.ai.entity;

import com.agri.claim.common.core.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_segment_result")
public class SegmentResult extends BaseEntity {

    private Long taskId;
    private Long imageId;
    private String modelName;
    private String modelVersion;
    private String segmentClass;
    private BigDecimal confidence;
    private String polygonWkt;
    private String maskPath;
    private BigDecimal area;
    private String cropType;
    private BigDecimal cropConfidence;
    private String status;
    private String remark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private String metadata;
}

package com.agri.claim.ai.entity;

import com.agri.claim.common.core.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_change_detect_result")
public class ChangeDetectResult extends BaseEntity {

    private Long taskId;
    private Long beforeImageId;
    private Long afterImageId;
    private String modelName;
    private String modelVersion;
    private String disasterType;
    private String disasterLevel;
    private String maskPath;
    private BigDecimal disasterArea;
    private BigDecimal disasterRatio;
    private BigDecimal ndviBefore;
    private BigDecimal ndviAfter;
    private BigDecimal ndviDiff;
    private String status;
    private String remark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private String metadata;
}

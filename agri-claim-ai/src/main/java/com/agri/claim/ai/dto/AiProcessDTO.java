package com.agri.claim.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AiProcessDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    private Long imageId;

    private Long beforeImageId;

    private Long afterImageId;

    private List<String> processTypes;

    private String segmentModel = "UNet++_V2";

    private String classifyModel = "ResNet50_V1";

    private String changeDetectModel = "ChangeFormer_V1";

    private Integer minFarmlandArea = 100;

    private Double ndviThreshold = 0.25;

    private String remark;
}

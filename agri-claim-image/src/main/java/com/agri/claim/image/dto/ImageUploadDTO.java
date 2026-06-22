package com.agri.claim.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ImageUploadDTO {

    @NotBlank(message = "影像类型不能为空")
    private String imageType;

    private String disasterType;

    private String shootTime;

    private String location;

    private Long missionId;

    private String missionName;

    private BigDecimal centerLon;

    private BigDecimal centerLat;

    private String remark;

    @NotNull(message = "文件不能为空")
    private MultipartFile file;

    private String uploadId;
    private Integer chunkIndex;
    private Integer totalChunks;
    private String fileName;
    private Long totalSize;
}

package com.agri.claim.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ChunkUploadDTO {

    @NotBlank(message = "上传任务ID不能为空")
    private String uploadId;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "分片序号不能为空")
    private Integer chunkIndex;

    @NotNull(message = "总分片数不能为空")
    private Integer totalChunks;

    @NotNull(message = "分片大小不能为空")
    private Long chunkSize;

    private Long totalSize;

    private String md5;

    private String imageType;

    private String disasterType;

    private Long missionId;

    private String missionName;

    private String location;

    private java.math.BigDecimal centerLon;

    private java.math.BigDecimal centerLat;

    private MultipartFile file;
}

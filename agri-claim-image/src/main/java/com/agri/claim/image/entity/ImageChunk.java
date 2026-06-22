package com.agri.claim.image.entity;

import com.agri.claim.common.core.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("image_chunk")
public class ImageChunk extends BaseEntity {

    private String uploadId;
    private Long imageId;
    private String fileName;
    private String bucketName;
    private String objectKey;
    private Integer chunkIndex;
    private Integer totalChunks;
    private Long chunkSize;
    private String md5;
    private Integer status;
}

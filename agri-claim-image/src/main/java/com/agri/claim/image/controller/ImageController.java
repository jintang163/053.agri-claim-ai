package com.agri.claim.image.controller;

import cn.hutool.core.util.StrUtil;
import com.agri.claim.common.config.MinioConfig;
import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.core.page.PageResult;
import com.agri.claim.common.result.R;
import com.agri.claim.image.dto.ChunkUploadDTO;
import com.agri.claim.image.dto.ImageUploadDTO;
import com.agri.claim.image.entity.ImageChunk;
import com.agri.claim.image.entity.ImageInfo;
import com.agri.claim.image.service.ChunkUploadService;
import com.agri.claim.image.service.GdalPreprocessService;
import com.agri.claim.image.service.ImageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "影像管理")
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final GdalPreprocessService gdalPreprocessService;
    private final MinioConfig minioConfig;
    private final ChunkUploadService chunkUploadService;

    @Operation(summary = "影像上传")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ImageInfo> upload(@ModelAttribute ImageUploadDTO dto) {
        return R.ok("上传成功", imageService.uploadImage(dto));
    }

    @Operation(summary = "批量上传影像")
    @PostMapping(value = "/batchUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<List<ImageInfo>> batchUpload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "imageType") String imageType,
            @RequestParam(value = "disasterType", required = false) String disasterType,
            @RequestParam(value = "missionId", required = false) Long missionId,
            @RequestParam(value = "missionName", required = false) String missionName) {
        List<ImageInfo> result = files.stream().map(file -> {
            ImageUploadDTO dto = new ImageUploadDTO();
            dto.setImageType(imageType);
            dto.setDisasterType(disasterType);
            dto.setMissionId(missionId);
            dto.setMissionName(missionName);
            dto.setFile(file);
            return imageService.uploadImage(dto);
        }).toList();
        return R.ok("批量上传成功", result);
    }

    @Operation(summary = "分页查询影像列表")
    @GetMapping("/list")
    public R<PageResult<ImageInfo>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String imageType,
            @RequestParam(required = false) String imageStatus,
            @RequestParam(required = false) String disasterType,
            @RequestParam(required = false) String keyword) {
        IPage<ImageInfo> page = imageService.pageList(pageNum, pageSize, imageType, imageStatus,
                disasterType, keyword);
        return R.ok(PageResult.of(page));
    }

    @Operation(summary = "获取影像详情")
    @GetMapping("/{id}")
    public R<Map<String, Object>> getDetail(@PathVariable Long id) {
        ImageInfo info = imageService.getById(id);
        if (info == null) return R.fail("影像不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("info", info);
        result.put("previewUrl", imageService.getPreviewUrl(id));
        result.put("thumbnailUrl", imageService.getThumbnailUrl(id));
        if (StrUtil.isNotBlank(info.getDomKey())) {
            try {
                result.put("domUrl", minioConfig.getPresignedUrl(
                        info.getBucketName(), info.getDomKey(), 1440));
            } catch (Exception ignored) {
            }
        }
        return R.ok(result);
    }

    @Operation(summary = "手动触发影像预处理")
    @PostMapping("/preprocess/{id}")
    public R<ImageInfo> preprocess(@PathVariable Long id) {
        return R.ok("预处理任务已提交", imageService.preprocessImage(id));
    }

    @Operation(summary = "获取影像预览地址")
    @GetMapping("/preview/{id}")
    public R<String> preview(@PathVariable Long id) {
        return R.ok(imageService.getPreviewUrl(id));
    }

    @Operation(summary = "获取缩略图地址")
    @GetMapping("/thumbnail/{id}")
    public R<String> thumbnail(@PathVariable Long id) {
        return R.ok(imageService.getThumbnailUrl(id));
    }

    @Operation(summary = "下载原始影像")
    @SneakyThrows
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ImageInfo info = imageService.getById(id);
        if (info == null) return ResponseEntity.notFound().build();
        try (InputStream is = minioConfig.getFile(info.getBucketName(), info.getObjectKey())) {
            byte[] bytes = is.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + info.getOriginalName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(bytes.length)
                    .body(bytes);
        }
    }

    @Operation(summary = "获取任务下的影像列表")
    @GetMapping("/mission/{missionId}")
    public R<List<ImageInfo>> getByMission(
            @PathVariable Long missionId,
            @RequestParam(required = false) String imageType) {
        return R.ok(imageService.getByMissionId(missionId, imageType));
    }

    @Operation(summary = "计算NDVI植被指数")
    @GetMapping("/ndvi")
    public R<Map<String, Object>> calcNdvi(
            @RequestParam Long beforeImageId,
            @RequestParam Long afterImageId) {
        BigDecimal ndvi = gdalPreprocessService.calculateNdvi(beforeImageId, afterImageId);
        ImageInfo before = imageService.getById(beforeImageId);
        ImageInfo after = imageService.getById(afterImageId);
        Map<String, Object> result = new HashMap<>();
        result.put("ndviValue", ndvi);
        result.put("beforeImageId", beforeImageId);
        result.put("afterImageId", afterImageId);
        result.put("beforeUrl", before != null ? imageService.getPreviewUrl(beforeImageId) : null);
        result.put("afterUrl", after != null ? imageService.getPreviewUrl(afterImageId) : null);
        result.put("analysisLevel", analyzeNdviLevel(ndvi));
        return R.ok(result);
    }

    @Operation(summary = "删除影像")
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Long> ids) {
        ids.forEach(id -> {
            ImageInfo info = imageService.getById(id);
            if (info != null) {
                try {
                    minioConfig.removeFile(info.getBucketName(), info.getObjectKey());
                } catch (Exception ignored) {
                }
            }
        });
        imageService.removeByIds(ids);
        return R.ok("删除成功");
    }

    private String analyzeNdviLevel(BigDecimal ndvi) {
        double val = ndvi.doubleValue();
        if (val < 0.1) return "重度受灾";
        if (val < 0.25) return "中度受灾";
        if (val < 0.4) return "轻度受灾";
        if (val < 0.55) return "轻微受影响";
        return "正常生长";
    }

    @Operation(summary = "初始化分片上传")
    @PostMapping("/chunk/init")
    public R<Map<String, Object>> initChunkUpload(@RequestParam String fileName) {
        return R.ok(chunkUploadService.initUpload(fileName));
    }

    @Operation(summary = "检查已上传分片")
    @GetMapping("/chunk/check")
    public R<List<Integer>> checkUploadedChunks(@RequestParam String uploadId) {
        return R.ok(chunkUploadService.checkUploadedChunks(uploadId));
    }

    @Operation(summary = "上传分片")
    @PostMapping(value = "/chunk/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ImageChunk> uploadChunk(@ModelAttribute ChunkUploadDTO dto) {
        return R.ok("分片上传成功", chunkUploadService.uploadChunk(dto));
    }

    @Operation(summary = "合并分片完成上传")
    @PostMapping("/chunk/merge")
    public R<ImageInfo> mergeChunks(@RequestBody ChunkUploadDTO dto) {
        return R.ok("合并完成", chunkUploadService.mergeChunks(dto));
    }
}

package com.agri.claim.common.config;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName = "agri-images";

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        initBucket(client);
        return client;
    }

    private void initBucket(MinioClient client) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket [{}] created successfully", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO bucket init failed", e);
        }
    }

    public String uploadFile(MultipartFile file, String bucket, String objectName) throws Exception {
        MinioClient client = minioClient();
        client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType() != null ? file.getContentType() :
                        MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .build());
        return objectName;
    }

    public String uploadFile(InputStream inputStream, long size, String bucket,
                              String objectName, String contentType) throws Exception {
        MinioClient client = minioClient();
        client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(inputStream, size, -1)
                .contentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .build());
        return objectName;
    }

    public InputStream getFile(String bucket, String objectName) throws Exception {
        MinioClient client = minioClient();
        return client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
    }

    public void removeFile(String bucket, String objectName) throws Exception {
        MinioClient client = minioClient();
        client.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
    }

    public String getPresignedUrl(String bucket, String objectName, int expireMinutes) throws Exception {
        MinioClient client = minioClient();
        return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectName)
                .expiry(expireMinutes, TimeUnit.MINUTES)
                .build());
    }

    public boolean fileExists(String bucket, String objectName) {
        try {
            MinioClient client = minioClient();
            client.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

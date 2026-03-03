package com.xueying.jobapplicationtracker.service.impl;

import com.xueying.jobapplicationtracker.service.MinIOService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;

/**
 * Default MinIO-backed storage adapter.
 */
@Service
public class MinIOServiceImpl implements MinIOService {
    private final MinioClient minioClient;
    private final String bucket;

    public MinIOServiceImpl(@Value("${minio.endpoint}") String endpoint,
                            @Value("${minio.accessKey}") String accessKey,
                            @Value("${minio.secretKey}") String secretKey,
                            @Value("${minio.bucket}") String bucket) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
    }

    @PostConstruct
    public void init() {
        try {
            createBucketIfNotExists();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MinIO bucket: " + e.getMessage(), e);
        }
    }

    @Override
    public void createBucketIfNotExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    @Override
    public String upload(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        createBucketIfNotExists();
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );
        return objectName;
    }

    @Override
    public InputStream download(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }

    @Override
    public void delete(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }
}


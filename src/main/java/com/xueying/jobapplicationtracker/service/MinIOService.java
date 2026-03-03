package com.xueying.jobapplicationtracker.service;

import java.io.InputStream;

/**
 * Thin storage abstraction for MinIO object operations.
 */
public interface MinIOService {
    void createBucketIfNotExists() throws Exception;

    String upload(String objectName, InputStream inputStream, long size, String contentType) throws Exception;

    InputStream download(String objectName) throws Exception;

    void delete(String objectName) throws Exception;
}


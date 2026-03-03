package com.xueying.jobapplicationtracker.service;

import com.xueying.jobapplicationtracker.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * Manages attachments linked to job applications.
 */
public interface DocumentService {
    List<DocumentVO> listDocuments();

    List<DocumentVO> listByApplicationId(Long applicationId);

    Long upload(Long applicationId, MultipartFile file) throws Exception;

    DownloadPayload loadForDownload(Long documentId) throws Exception;

    DownloadPayload loadForDownload(Long applicationId, Long documentId) throws Exception;

    boolean delete(Long applicationId, Long documentId) throws Exception;

    boolean delete(Long documentId) throws Exception;

    class DownloadPayload {
        private final String originalFilename;
        private final String contentType;
        private final Long sizeBytes;
        private final InputStream inputStream;

        public DownloadPayload(String originalFilename, String contentType, Long sizeBytes, InputStream inputStream) {
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
            this.inputStream = inputStream;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public String getContentType() {
            return contentType;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }

        public InputStream getInputStream() {
            return inputStream;
        }
    }
}


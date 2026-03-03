package com.xueying.jobapplicationtracker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xueying.jobapplicationtracker.entity.ApplicationEntity;
import com.xueying.jobapplicationtracker.entity.DocumentEntity;
import com.xueying.jobapplicationtracker.mapper.ApplicationMapper;
import com.xueying.jobapplicationtracker.mapper.DocumentMapper;
import com.xueying.jobapplicationtracker.service.DocumentService;
import com.xueying.jobapplicationtracker.service.MinIOService;
import com.xueying.jobapplicationtracker.service.UserService;
import com.xueying.jobapplicationtracker.vo.DocumentVO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Attachment service with per-user ownership checks on every data operation.
 */
@Service
public class DocumentServiceImpl implements DocumentService {
    private final DocumentMapper documentMapper;
    private final ApplicationMapper applicationMapper;
    private final MinIOService minIOService;
    private final UserService userService;

    public DocumentServiceImpl(DocumentMapper documentMapper,
                               ApplicationMapper applicationMapper,
                               MinIOService minIOService,
                               UserService userService) {
        this.documentMapper = documentMapper;
        this.applicationMapper = applicationMapper;
        this.minIOService = minIOService;
        this.userService = userService;
    }

    @Override
    public List<DocumentVO> listDocuments() {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<DocumentEntity> query = new LambdaQueryWrapper<>();
        query.eq(DocumentEntity::getUserId, userId)
                .orderByDesc(DocumentEntity::getCreatedAt)
                .orderByDesc(DocumentEntity::getId);
        return toVOList(documentMapper.selectList(query));
    }

    @Override
    public List<DocumentVO> listByApplicationId(Long applicationId) {
        Long userId = currentUserId();
        if (applicationId == null || userId == null || !isOwnedApplication(applicationId, userId)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<DocumentEntity> query = new LambdaQueryWrapper<>();
        query.eq(DocumentEntity::getUserId, userId)
                .eq(DocumentEntity::getApplicationId, applicationId)
                .orderByDesc(DocumentEntity::getCreatedAt)
                .orderByDesc(DocumentEntity::getId);
        return toVOList(documentMapper.selectList(query));
    }

    @Override
    /**
     * Uploads file content to MinIO and stores metadata bound to one application.
     */
    public Long upload(Long applicationId, MultipartFile file) throws Exception {
        Long userId = currentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("Login required.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file selected.");
        }
        if (applicationId == null) {
            throw new IllegalArgumentException("Attachment must be linked to an application.");
        }
        if (!isOwnedApplication(applicationId, userId)) {
            throw new IllegalArgumentException("Selected application does not exist.");
        }

        String filename = sanitizeFilename(file.getOriginalFilename());
        String objectKey = buildObjectKey(filename);
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        try (InputStream inputStream = file.getInputStream()) {
            minIOService.upload(objectKey, inputStream, file.getSize(), contentType);
        }

        DocumentEntity document = new DocumentEntity();
        document.setUserId(userId);
        document.setApplicationId(applicationId);
        document.setOriginalFilename(filename);
        document.setObjectKey(objectKey);
        document.setContentType(contentType);
        document.setSizeBytes(file.getSize());
        document.setCreatedAt(LocalDateTime.now());
        documentMapper.insert(document);
        return document.getId();
    }

    @Override
    public DownloadPayload loadForDownload(Long documentId) throws Exception {
        return loadForDownloadInternal(null, documentId, false);
    }

    @Override
    public DownloadPayload loadForDownload(Long applicationId, Long documentId) throws Exception {
        return loadForDownloadInternal(applicationId, documentId, true);
    }

    @Override
    public boolean delete(Long applicationId, Long documentId) throws Exception {
        Long userId = currentUserId();
        if (userId == null || applicationId == null || documentId == null) {
            return false;
        }
        DocumentEntity document = findOwnedDocument(documentId, userId);
        if (document == null
                || !isOwnedByApplication(document, applicationId)) {
            return false;
        }

        minIOService.delete(document.getObjectKey());
        return documentMapper.deleteById(documentId) > 0;
    }

    @Override
    public boolean delete(Long documentId) throws Exception {
        Long userId = currentUserId();
        if (userId == null || documentId == null) {
            return false;
        }
        DocumentEntity document = findOwnedDocument(documentId, userId);
        if (document == null) {
            return false;
        }

        minIOService.delete(document.getObjectKey());
        return documentMapper.deleteById(documentId) > 0;
    }

    /**
     * Returns attachment stream only when the current user owns the document (and optionally the application).
     */
    private DownloadPayload loadForDownloadInternal(Long applicationId, Long documentId, boolean requireOwnership) throws Exception {
        Long userId = currentUserId();
        if (userId == null || documentId == null) {
            return null;
        }

        DocumentEntity document = documentMapper.selectById(documentId);
        if (document == null || document.getUserId() == null || !userId.equals(document.getUserId())) {
            return null;
        }
        if (requireOwnership && !isOwnedByApplication(document, applicationId)) {
            return null;
        }

        InputStream inputStream = minIOService.download(document.getObjectKey());
        return new DownloadPayload(
                sanitizeFilename(document.getOriginalFilename()),
                StringUtils.hasText(document.getContentType()) ? document.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE,
                document.getSizeBytes(),
                inputStream
        );
    }

    private boolean isOwnedByApplication(DocumentEntity document, Long applicationId) {
        return document != null
                && applicationId != null
                && document.getApplicationId() != null
                && applicationId.equals(document.getApplicationId());
    }

    private List<DocumentVO> toVOList(List<DocumentEntity> documents) {
        Map<Long, String> applicationLabels = resolveApplicationLabels(documents);
        return documents.stream().map(item -> {
            DocumentVO vo = new DocumentVO();
            vo.setId(item.getId());
            vo.setApplicationId(item.getApplicationId());
            vo.setOriginalFilename(item.getOriginalFilename());
            vo.setContentType(item.getContentType());
            vo.setSizeBytes(item.getSizeBytes());
            vo.setCreatedAt(item.getCreatedAt());
            vo.setApplicationLabel(applicationLabels.getOrDefault(item.getApplicationId(), "Unlinked"));
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, String> resolveApplicationLabels(List<DocumentEntity> documents) {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyMap();
        }
        Set<Long> appIds = documents.stream()
                .map(DocumentEntity::getApplicationId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (appIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getUserId, userId).in(ApplicationEntity::getId, appIds);
        List<ApplicationEntity> applications = applicationMapper.selectList(query);

        Map<Long, String> labels = new HashMap<>();
        for (ApplicationEntity application : applications) {
            String company = trim(application.getCompany());
            String role = trim(application.getRole());
            labels.put(application.getId(), company + " / " + role);
        }
        return labels;
    }

    private String sanitizeFilename(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename.trim());
        if (!StringUtils.hasText(cleaned) || ".".equals(cleaned) || "..".equals(cleaned)) {
            return "attachment.bin";
        }
        return cleaned.replaceAll("[\\\\/]", "_");
    }

    private String buildObjectKey(String filename) {
        return "attachments/" + LocalDate.now() + "/" + UUID.randomUUID() + "-" + filename;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isOwnedApplication(Long applicationId, Long userId) {
        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getId, applicationId)
                .eq(ApplicationEntity::getUserId, userId)
                .last("LIMIT 1");
        return applicationMapper.selectOne(query) != null;
    }

    private Long currentUserId() {
        return userService.currentUserId();
    }

    private DocumentEntity findOwnedDocument(Long documentId, Long userId) {
        DocumentEntity document = documentMapper.selectById(documentId);
        if (document == null || document.getUserId() == null || !userId.equals(document.getUserId())) {
            return null;
        }
        return document;
    }
}


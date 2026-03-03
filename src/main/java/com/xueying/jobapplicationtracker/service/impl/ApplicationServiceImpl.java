package com.xueying.jobapplicationtracker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xueying.jobapplicationtracker.dto.ApplicationDTO;
import com.xueying.jobapplicationtracker.dto.ApplicationEditDTO;
import com.xueying.jobapplicationtracker.dto.ApplicationQueryDTO;
import com.xueying.jobapplicationtracker.entity.ApplicationEntity;
import com.xueying.jobapplicationtracker.mapper.ApplicationMapper;
import com.xueying.jobapplicationtracker.service.ApplicationService;
import com.xueying.jobapplicationtracker.service.UserService;
import com.xueying.jobapplicationtracker.vo.ApplicationVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MyBatis-Plus implementation of application management scoped to the logged-in user.
 */
@Service
public class ApplicationServiceImpl implements ApplicationService {
    private static final List<String> SUPPORTED_REGIONS = Arrays.asList("UK", "China", "EU", "Other");
    private static final List<String> SUPPORTED_STATUSES = Arrays.asList("Applied", "Interviewing", "Offer", "Rejected");

    private final ApplicationMapper mapper;
    private final UserService userService;

    public ApplicationServiceImpl(ApplicationMapper mapper, UserService userService) {
        this.mapper = mapper;
        this.userService = userService;
    }

    /**
     * Returns filtered application rows for the current user.
     */
    @Override
    public List<ApplicationVO> list(ApplicationQueryDTO queryDTO) {
        Long userId = currentUserId();
        if (userId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getUserId, userId);
        if (queryDTO != null) {
            String keyword = trim(queryDTO.getKeyword());
            String status = trim(queryDTO.getStatus());
            if (!keyword.isEmpty()) {
                query.and(w -> w.like(ApplicationEntity::getCompany, keyword)
                        .or().like(ApplicationEntity::getRole, keyword)
                        .or().like(ApplicationEntity::getStatus, keyword)
                        .or().like(ApplicationEntity::getRegion, keyword)
                        .or().like(ApplicationEntity::getCountry, keyword)
                        .or().like(ApplicationEntity::getCompanySummary, keyword)
                        .or().like(ApplicationEntity::getLink, keyword)
                        .or().like(ApplicationEntity::getNotes, keyword));
            }
            if (!status.isEmpty()) {
                query.eq(ApplicationEntity::getStatus, status);
            }
        }
        query.orderByDesc(ApplicationEntity::getUpdatedAt).orderByDesc(ApplicationEntity::getId);
        return mapper.selectList(query).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ApplicationVO findById(Long id) {
        Long userId = currentUserId();
        if (id == null || userId == null) {
            return null;
        }
        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getId, id).eq(ApplicationEntity::getUserId, userId).last("LIMIT 1");
        ApplicationEntity entity = mapper.selectOne(query);
        return entity == null ? null : toVO(entity);
    }

    /**
     * Persists a new application for the current user.
     */
    @Override
    public Long create(ApplicationDTO dto) {
        Long userId = currentUserId();
        if (userId == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();

        ApplicationEntity entity = new ApplicationEntity();
        entity.setUserId(userId);
        entity.setCompany(trim(dto.getCompany()));
        entity.setRole(trim(dto.getRole()));
        entity.setRegion(normalizeRegion(dto.getRegion()));
        entity.setStatus(defaultStatus(dto.getStatus()));
        entity.setAppliedDate(dto.getAppliedDate() == null ? LocalDate.now() : dto.getAppliedDate());
        entity.setLink(trim(dto.getLink()));
        entity.setNotes(trim(dto.getNotes()));
        entity.setCompanySummary(trim(dto.getCompanySummary()));
        entity.setCountry(trim(dto.getCountry()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        mapper.insert(entity);
        return entity.getId();
    }

    /**
     * Updates an existing application only when it belongs to the current user.
     */
    @Override
    public boolean update(ApplicationEditDTO dto) {
        Long userId = currentUserId();
        if (userId == null) {
            return false;
        }
        if (dto == null || dto.getId() == null) {
            return false;
        }
        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getId, dto.getId()).eq(ApplicationEntity::getUserId, userId).last("LIMIT 1");
        ApplicationEntity current = mapper.selectOne(query);
        if (current == null) {
            return false;
        }

        current.setCompany(trim(dto.getCompany()));
        current.setRole(trim(dto.getRole()));
        current.setRegion(normalizeRegion(dto.getRegion()));
        current.setStatus(defaultStatus(dto.getStatus()));
        current.setAppliedDate(dto.getAppliedDate() == null ? current.getAppliedDate() : dto.getAppliedDate());
        current.setLink(trim(dto.getLink()));
        current.setNotes(trim(dto.getNotes()));
        current.setCompanySummary(trim(dto.getCompanySummary()));
        current.setCountry(trim(dto.getCountry()));
        current.setUpdatedAt(LocalDateTime.now());

        return mapper.updateById(current) > 0;
    }

    @Override
    public boolean delete(Long id) {
        Long userId = currentUserId();
        if (id == null || userId == null) {
            return false;
        }
        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getId, id).eq(ApplicationEntity::getUserId, userId);
        return mapper.delete(query) > 0;
    }

    @Override
    public List<String> supportedRegions() {
        return new ArrayList<>(SUPPORTED_REGIONS);
    }

    @Override
    public List<String> supportedStatuses() {
        return new ArrayList<>(SUPPORTED_STATUSES);
    }

    @Override
    public Map<String, Map<String, Long>> regionStatusSummary() {
        Map<String, Map<String, Long>> summary = initRegionSummary();
        Long userId = currentUserId();
        if (userId == null) {
            return summary;
        }
        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getUserId, userId);
        List<ApplicationEntity> all = mapper.selectList(query);

        for (ApplicationEntity entity : all) {
            String region = normalizeRegion(entity.getRegion());
            String status = defaultStatus(entity.getStatus());

            Map<String, Long> statusCount = summary.computeIfAbsent(region, k -> initStatusCounter());
            statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1L);
        }
        return summary;
    }

    @Override
    public List<ApplicationVO> listRecentAdded(int limit) {
        Long userId = currentUserId();
        if (userId == null) {
            return new ArrayList<>();
        }
        int safeLimit = sanitizeLimit(limit);
        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getUserId, userId);
        query.orderByDesc(ApplicationEntity::getCreatedAt).orderByDesc(ApplicationEntity::getId);
        query.last("LIMIT " + safeLimit);
        return mapper.selectList(query).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public long countCurrentUserApplications() {
        Long userId = currentUserId();
        if (userId == null) {
            return 0L;
        }
        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getUserId, userId);
        return mapper.selectCount(query);
    }

    private ApplicationVO toVO(ApplicationEntity entity) {
        ApplicationVO vo = new ApplicationVO();
        vo.setId(entity.getId());
        vo.setCompany(entity.getCompany());
        vo.setRole(entity.getRole());
        vo.setRegion(normalizeRegion(entity.getRegion()));
        vo.setStatus(defaultStatus(entity.getStatus()));
        vo.setAppliedDate(entity.getAppliedDate());
        vo.setLink(entity.getLink());
        vo.setNotes(entity.getNotes());
        vo.setCompanySummary(entity.getCompanySummary());
        vo.setCountry(entity.getCountry());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Map<String, Map<String, Long>> initRegionSummary() {
        Map<String, Map<String, Long>> summary = new LinkedHashMap<>();
        for (String region : SUPPORTED_REGIONS) {
            summary.put(region, initStatusCounter());
        }
        return summary;
    }

    private Map<String, Long> initStatusCounter() {
        Map<String, Long> counter = new LinkedHashMap<>();
        for (String status : SUPPORTED_STATUSES) {
            counter.put(status, 0L);
        }
        return counter;
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private Long currentUserId() {
        return userService.currentUserId();
    }

    private String normalizeRegion(String region) {
        String normalized = trim(region);
        if (normalized.equalsIgnoreCase("uk") || normalized.equalsIgnoreCase("united kingdom")) {
            return "UK";
        }
        if (normalized.equalsIgnoreCase("china") || normalized.equalsIgnoreCase("cn")) {
            return "China";
        }
        if (normalized.equalsIgnoreCase("eu")
                || normalized.equalsIgnoreCase("european union")
                || normalized.equalsIgnoreCase("europe")) {
            return "EU";
        }
        return "Other";
    }

    private String defaultStatus(String status) {
        String normalized = trim(status);
        if (normalized.isEmpty()) {
            return "Applied";
        }
        for (String supportedStatus : SUPPORTED_STATUSES) {
            if (supportedStatus.equalsIgnoreCase(normalized)) {
                return supportedStatus;
            }
        }
        return "Applied";
    }
}


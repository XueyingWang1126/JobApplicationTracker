package com.xueying.jobapplicationtracker.service;

import com.xueying.jobapplicationtracker.dto.ApplicationDTO;
import com.xueying.jobapplicationtracker.dto.ApplicationEditDTO;
import com.xueying.jobapplicationtracker.dto.ApplicationQueryDTO;
import com.xueying.jobapplicationtracker.vo.ApplicationVO;

import java.util.List;
import java.util.Map;

/**
 * Handles CRUD and summary views for the current authenticated user's applications.
 */
public interface ApplicationService {
    List<ApplicationVO> list(ApplicationQueryDTO queryDTO);

    ApplicationVO findById(Long id);

    Long create(ApplicationDTO dto);

    boolean update(ApplicationEditDTO dto);

    boolean delete(Long id);

    List<String> supportedRegions();

    List<String> supportedStatuses();

    Map<String, Map<String, Long>> regionStatusSummary();

    List<ApplicationVO> listRecentAdded(int limit);

    long countCurrentUserApplications();
}


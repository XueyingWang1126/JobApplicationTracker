package com.xueying.jobapplicationtracker.service;

import com.xueying.jobapplicationtracker.dto.CompanyAutoFillResponse;

/**
 * Fetches public company information to prefill application forms.
 */
public interface CompanyAutoFillService {
    CompanyAutoFillResponse autoFill(String companyName);
}


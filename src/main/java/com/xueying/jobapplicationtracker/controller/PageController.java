package com.xueying.jobapplicationtracker.controller;

import com.xueying.jobapplicationtracker.service.ApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders read-only overview pages.
 */
@Controller
public class PageController {
    private final ApplicationService applicationService;

    public PageController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping({"/", "/dashboard"})
    /**
     * Dashboard shows summary and recent items only, without CRUD forms.
     */
    public String dashboard(Model model) {
        Map<String, Map<String, Long>> summary = applicationService.regionStatusSummary();
        List<String> statuses = applicationService.supportedStatuses();

        model.addAttribute("regionStatusSummary", summary);
        model.addAttribute("statuses", statuses);
        model.addAttribute("regionStatusRows", buildRegionStatusRows(summary, statuses));
        model.addAttribute("recentAdded", applicationService.listRecentAdded(5));
        model.addAttribute("totalApplications", applicationService.countCurrentUserApplications());
        model.addAttribute("activeNav", "dashboard");
        return "dashboard";
    }

    private List<Map<String, Object>> buildRegionStatusRows(Map<String, Map<String, Long>> summary,
                                                            List<String> statuses) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (summary == null || statuses == null) {
            return rows;
        }

        for (Map.Entry<String, Map<String, Long>> entry : summary.entrySet()) {
            Map<String, Long> statusMap = entry.getValue() == null ? new LinkedHashMap<>() : entry.getValue();
            List<Long> counts = new ArrayList<>();
            for (String status : statuses) {
                counts.add(statusMap.getOrDefault(status, 0L));
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("region", entry.getKey());
            row.put("counts", counts);
            rows.add(row);
        }
        return rows;
    }
}


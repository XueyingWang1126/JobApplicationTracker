package com.xueying.jobapplicationtracker.controller;

import com.xueying.jobapplicationtracker.dto.ApplicationDTO;
import com.xueying.jobapplicationtracker.dto.ApplicationEditDTO;
import com.xueying.jobapplicationtracker.dto.ApplicationQueryDTO;
import com.xueying.jobapplicationtracker.dto.CompanyAutoFillResponse;
import com.xueying.jobapplicationtracker.service.ApplicationService;
import com.xueying.jobapplicationtracker.service.CompanyAutoFillService;
import com.xueying.jobapplicationtracker.service.DocumentService;
import com.xueying.jobapplicationtracker.utils.Result;
import com.xueying.jobapplicationtracker.vo.ApplicationVO;
import com.xueying.jobapplicationtracker.vo.DocumentVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles application management flows, including create/edit/delete and company auto-fill.
 */
@Controller
@RequestMapping("/applications")
public class ApplicationController {
    private final ApplicationService applicationService;
    private final CompanyAutoFillService companyAutoFillService;
    private final DocumentService documentService;

    public ApplicationController(ApplicationService applicationService,
                                 CompanyAutoFillService companyAutoFillService,
                                 DocumentService documentService) {
        this.applicationService = applicationService;
        this.companyAutoFillService = companyAutoFillService;
        this.documentService = documentService;
    }

    @GetMapping
    public String list(@ModelAttribute("query") ApplicationQueryDTO query,
                       @ModelAttribute("message") String message,
                       Model model) {
        renderPage(model, query, defaultCreateForm(), new ApplicationEditDTO(), false, message);
        return "applications";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id,
                         @ModelAttribute("message") String message,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        ApplicationVO application = applicationService.findById(id);
        if (application == null) {
            redirectAttributes.addFlashAttribute("message", "Application was not found.");
            return "redirect:/applications";
        }

        List<DocumentVO> documents = documentService.listByApplicationId(id);
        model.addAttribute("appDetail", application);
        model.addAttribute("documents", documents);
        model.addAttribute("activeNav", "applications");
        model.addAttribute("message", message == null ? "" : message);
        return "application-detail";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") Long id,
                       @ModelAttribute("query") ApplicationQueryDTO query,
                       @ModelAttribute("message") String message,
                       Model model) {
        ApplicationVO target = applicationService.findById(id);
        if (target == null) {
            renderPage(model, query, defaultCreateForm(), new ApplicationEditDTO(), false, "Application was not found.");
            return "applications";
        }
        ApplicationEditDTO editForm = toEditDTO(target);
        renderPage(model, query, defaultCreateForm(), editForm, true, message);
        return "applications";
    }

    @PostMapping("/create")
    /**
     * Creates one application and optionally uploads multiple attachments in the same transaction flow.
     */
    public String create(@ModelAttribute("createForm") ApplicationDTO createForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
                         @ModelAttribute("query") ApplicationQueryDTO query,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String error = validateCreateForm(createForm, bindingResult);
        if (error != null) {
            renderPage(model, query, createForm, new ApplicationEditDTO(), false, error);
            return "applications";
        }
        Long applicationId = applicationService.create(createForm);
        if (applicationId == null) {
            redirectAttributes.addFlashAttribute("message", "Application create failed.");
            return "redirect:/applications";
        }
        String message = buildCreateMessage(uploadAttachments(applicationId, attachments));
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/applications";
    }

    @PostMapping("/{id}/edit")
    /**
     * Updates one application and appends optional new attachments.
     */
    public String update(@PathVariable("id") Long id,
                         @ModelAttribute("editForm") ApplicationEditDTO editForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
                         @ModelAttribute("query") ApplicationQueryDTO query,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        editForm.setId(id);
        String error = validateEditForm(editForm, bindingResult);
        if (error != null) {
            renderPage(model, query, defaultCreateForm(), editForm, true, error);
            return "applications";
        }
        boolean updated = applicationService.update(editForm);
        if (!updated) {
            redirectAttributes.addFlashAttribute("message", "Application was not found.");
            return "redirect:/applications";
        }
        String message = buildUpdateMessage(uploadAttachments(id, attachments));
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/applications";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = applicationService.delete(id);
        redirectAttributes.addFlashAttribute("message", deleted ? "Application deleted." : "Application was not found.");
        return "redirect:/applications";
    }

    @GetMapping("/autofill")
    @ResponseBody
    public Result<CompanyAutoFillResponse> autoFill(@RequestParam("company") String company) {
        try {
            return Result.ok(companyAutoFillService.autoFill(company));
        } catch (Exception e) {
            return Result.ok(companyAutoFillService.autoFill(""));
        }
    }

    private void renderPage(Model model,
                            ApplicationQueryDTO query,
                            ApplicationDTO createForm,
                            ApplicationEditDTO editForm,
                            boolean editMode,
                            String message) {
        List<ApplicationVO> applications = applicationService.list(query);
        model.addAttribute("applications", applications);
        model.addAttribute("regionGroups", groupByRegion(applications));
        model.addAttribute("query", query == null ? new ApplicationQueryDTO() : query);
        model.addAttribute("createForm", createForm == null ? defaultCreateForm() : createForm);
        model.addAttribute("editForm", editForm == null ? new ApplicationEditDTO() : editForm);
        model.addAttribute("editMode", editMode);
        List<DocumentVO> editDocuments = new ArrayList<>();
        if (editMode && editForm != null && editForm.getId() != null) {
            editDocuments = documentService.listByApplicationId(editForm.getId());
        }
        model.addAttribute("editDocuments", editDocuments);
        model.addAttribute("regions", applicationService.supportedRegions());
        model.addAttribute("statuses", applicationService.supportedStatuses());
        model.addAttribute("activeNav", "applications");
        model.addAttribute("message", message == null ? "" : message);
    }

    private ApplicationDTO defaultCreateForm() {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setStatus("Applied");
        dto.setRegion("Other");
        dto.setAppliedDate(LocalDate.now());
        return dto;
    }

    private ApplicationEditDTO toEditDTO(ApplicationVO vo) {
        ApplicationEditDTO dto = new ApplicationEditDTO();
        dto.setId(vo.getId());
        dto.setCompany(vo.getCompany());
        dto.setRole(vo.getRole());
        dto.setRegion(vo.getRegion());
        dto.setStatus(vo.getStatus());
        dto.setAppliedDate(vo.getAppliedDate());
        dto.setLink(vo.getLink());
        dto.setNotes(vo.getNotes());
        dto.setCompanySummary(vo.getCompanySummary());
        dto.setCountry(vo.getCountry());
        return dto;
    }

    private String validateCreateForm(ApplicationDTO form, BindingResult bindingResult) {
        if (bindingResult != null && bindingResult.hasErrors()) {
            return "Please check the form values. Date must use yyyy-MM-dd.";
        }
        if (isBlank(form.getCompany()) || isBlank(form.getRole())) {
            return "Company and role are required.";
        }
        if (form.getAppliedDate() == null) {
            return "Applied date is required.";
        }
        return null;
    }

    private String validateEditForm(ApplicationEditDTO form, BindingResult bindingResult) {
        if (bindingResult != null && bindingResult.hasErrors()) {
            return "Please check the form values. Date must use yyyy-MM-dd.";
        }
        if (form.getId() == null) {
            return "Application id is required.";
        }
        if (isBlank(form.getCompany()) || isBlank(form.getRole())) {
            return "Company and role are required.";
        }
        if (form.getAppliedDate() == null) {
            return "Applied date is required.";
        }
        return null;
    }

    private Map<String, List<ApplicationVO>> groupByRegion(List<ApplicationVO> applications) {
        Map<String, List<ApplicationVO>> grouped = new LinkedHashMap<>();
        for (String region : applicationService.supportedRegions()) {
            grouped.put(region, new ArrayList<>());
        }
        if (applications == null) {
            return grouped;
        }
        for (ApplicationVO application : applications) {
            String region = application == null || isBlank(application.getRegion()) ? "Other" : application.getRegion();
            if (!grouped.containsKey(region)) {
                grouped.put(region, new ArrayList<>());
            }
            grouped.get(region).add(application);
        }
        return grouped;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private AttachmentUploadResult uploadAttachments(Long applicationId, MultipartFile[] attachments) {
        if (attachments == null || attachments.length == 0 || applicationId == null) {
            return AttachmentUploadResult.none();
        }
        int success = 0;
        int failed = 0;
        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) {
                continue;
            }
            try {
                documentService.upload(applicationId, attachment);
                success++;
            } catch (Exception ignored) {
                failed++;
            }
        }
        return new AttachmentUploadResult(success, failed);
    }

    private String buildCreateMessage(AttachmentUploadResult result) {
        if (result.successCount == 0 && result.failedCount == 0) {
            return "Application created.";
        }
        if (result.failedCount == 0) {
            return "Application created. " + result.successCount + " attachment(s) uploaded.";
        }
        return "Application created. " + result.successCount + " attachment(s) uploaded, "
                + result.failedCount + " failed.";
    }

    private String buildUpdateMessage(AttachmentUploadResult result) {
        if (result.successCount == 0 && result.failedCount == 0) {
            return "Application updated.";
        }
        if (result.failedCount == 0) {
            return "Application updated. " + result.successCount + " attachment(s) uploaded.";
        }
        return "Application updated. " + result.successCount + " attachment(s) uploaded, "
                + result.failedCount + " failed.";
    }

    private static class AttachmentUploadResult {
        private final int successCount;
        private final int failedCount;

        private AttachmentUploadResult(int successCount, int failedCount) {
            this.successCount = successCount;
            this.failedCount = failedCount;
        }

        private static AttachmentUploadResult none() {
            return new AttachmentUploadResult(0, 0);
        }
    }
}


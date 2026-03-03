package com.xueying.jobapplicationtracker.controller;

import com.xueying.jobapplicationtracker.service.DocumentService;
import com.xueying.jobapplicationtracker.vo.DocumentVO;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Serves attachment overview, download, and delete actions.
 */
@Controller
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/documents")
    public String documents(@ModelAttribute("message") String message, Model model) {
        List<DocumentVO> documents = documentService.listDocuments();
        model.addAttribute("documents", documents);
        model.addAttribute("activeNav", "documents");
        model.addAttribute("message", message == null ? "" : message);
        return "documents";
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable("id") Long id) {
        try {
            DocumentService.DownloadPayload payload = documentService.loadForDownload(id);
            return toDownloadResponse(payload);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/applications/{applicationId}/documents/{id}/download")
    public ResponseEntity<InputStreamResource> downloadInApplication(@PathVariable("applicationId") Long applicationId,
                                                                     @PathVariable("id") Long id) {
        try {
            DocumentService.DownloadPayload payload = documentService.loadForDownload(applicationId, id);
            return toDownloadResponse(payload);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/applications/{applicationId}/documents/{id}/delete")
    public String deleteInApplication(@PathVariable("applicationId") Long applicationId,
                                      @PathVariable("id") Long id,
                                      RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = documentService.delete(applicationId, id);
            redirectAttributes.addFlashAttribute("message", deleted ? "Attachment deleted." : "Attachment not found for this application.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Attachment delete failed.");
        }
        return "redirect:/applications/" + applicationId;
    }

    @PostMapping("/documents/{id}/delete")
    public String deleteFromDocuments(@PathVariable("id") Long id,
                                      RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = documentService.delete(id);
            redirectAttributes.addFlashAttribute("message", deleted ? "Attachment deleted." : "Attachment not found.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Attachment delete failed.");
        }
        return "redirect:/documents";
    }

    private ResponseEntity<InputStreamResource> toDownloadResponse(DocumentService.DownloadPayload payload) {
        if (payload == null) {
            return ResponseEntity.notFound().build();
        }
        String encodedName = URLEncoder.encode(payload.getOriginalFilename(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(payload.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName);
        if (payload.getSizeBytes() != null && payload.getSizeBytes() >= 0) {
            builder.contentLength(payload.getSizeBytes());
        }
        return builder.body(new InputStreamResource(payload.getInputStream()));
    }

}


package com.example.hotel.controller.admin;

import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.ConversationStatus;
import com.example.hotel.repository.ConversationRepository;
import com.example.hotel.service.JsonlExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class AdminExportController {

    private final ConversationRepository conversationRepository;
    private final JsonlExportService jsonlExportService;

    public AdminExportController(ConversationRepository conversationRepository,
                                 JsonlExportService jsonlExportService) {
        this.conversationRepository = conversationRepository;
        this.jsonlExportService = jsonlExportService;
    }

    @GetMapping("/admin/export")
    public String export(@RequestParam(required = false) ConversationCategory category,
                         @RequestParam(defaultValue = "APPROVED") ConversationStatus status,
                         Model model) {
        List<Conversation> conversations = filter(category, status);
        model.addAttribute("categories", ConversationCategory.values());
        model.addAttribute("statuses", ConversationStatus.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("preview", conversations.stream().limit(5).toList());
        model.addAttribute("totalMatches", conversations.size());
        return "admin/export";
    }

    @GetMapping("/admin/export/download")
    public ResponseEntity<byte[]> download(@RequestParam(required = false) ConversationCategory category,
                                           @RequestParam(defaultValue = "APPROVED") ConversationStatus status) {
        byte[] body = jsonlExportService.toJsonl(filter(category, status)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("hotel-conversations.jsonl")
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    private List<Conversation> filter(ConversationCategory category, ConversationStatus status) {
        if (category != null && status != null) {
            return conversationRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, status);
        }
        if (category != null) {
            return conversationRepository.findByCategoryOrderByCreatedAtDesc(category);
        }
        if (status != null) {
            return conversationRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return conversationRepository.findAllByOrderByCreatedAtDesc();
    }
}

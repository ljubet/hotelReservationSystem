package com.example.hotel.controller.admin;

import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.ConversationStatus;
import com.example.hotel.entity.MessageRole;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
                         @RequestParam(required = false) ConversationStatus status,
                         @RequestParam(required = false) LocalDate from,
                         @RequestParam(required = false) LocalDate to,
                         @RequestParam(defaultValue = "false") boolean aiCorrectedOnly,
                         @RequestParam(defaultValue = "false") boolean adminApprovedOnly,
                         @RequestParam(defaultValue = "JSONL") String format,
                         Model model) {
        List<Conversation> conversations = filter(category, status, from, to, aiCorrectedOnly, adminApprovedOnly);
        model.addAttribute("categories", ConversationCategory.values());
        model.addAttribute("statuses", ConversationStatus.values());
        model.addAttribute("formats", List.of("JSONL", "CSV", "PDF"));
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("aiCorrectedOnly", aiCorrectedOnly);
        model.addAttribute("adminApprovedOnly", adminApprovedOnly);
        model.addAttribute("selectedFormat", normalizeFormat(format));
        model.addAttribute("preview", conversations.stream().limit(3).map(this::toPreviewRow).toList());
        model.addAttribute("totalMatches", conversations.size());
        List<String> categoriesIncluded = categoriesIncluded(conversations);
        model.addAttribute("categoriesIncluded", categoriesIncluded);
        model.addAttribute("categoriesIncludedText", categoriesIncluded.isEmpty() ? "None" : String.join(", ", categoriesIncluded));
        return "admin/export";
    }

    @GetMapping("/admin/export/download")
    public ResponseEntity<byte[]> download(@RequestParam(required = false) ConversationCategory category,
                                           @RequestParam(required = false) ConversationStatus status,
                                           @RequestParam(required = false) LocalDate from,
                                           @RequestParam(required = false) LocalDate to,
                                           @RequestParam(defaultValue = "false") boolean aiCorrectedOnly,
                                           @RequestParam(defaultValue = "false") boolean adminApprovedOnly,
                                           @RequestParam(defaultValue = "JSONL") String format) {
        List<Conversation> conversations = filter(category, status, from, to, aiCorrectedOnly, adminApprovedOnly);
        String normalizedFormat = normalizeFormat(format);
        byte[] body = exportBody(conversations, normalizedFormat);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("hotel-conversations." + extension(normalizedFormat))
                        .build()
                        .toString())
                .contentType(contentType(normalizedFormat))
                .body(body);
    }

    private List<Conversation> filter(ConversationCategory category,
                                      ConversationStatus status,
                                      LocalDate from,
                                      LocalDate to,
                                      boolean aiCorrectedOnly,
                                      boolean adminApprovedOnly) {
        return conversationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(conversation -> category == null || conversation.getCategory() == category)
                .filter(conversation -> status == null || conversation.getStatus() == status)
                .filter(conversation -> !adminApprovedOnly || conversation.getStatus() == ConversationStatus.APPROVED)
                .filter(conversation -> !aiCorrectedOnly || conversation.isAiCorrected())
                .filter(conversation -> from == null || conversation.getCreatedAt() == null || !conversation.getCreatedAt().toLocalDate().isBefore(from))
                .filter(conversation -> to == null || conversation.getCreatedAt() == null || !conversation.getCreatedAt().toLocalDate().isAfter(to))
                .toList();
    }

    private List<String> categoriesIncluded(List<Conversation> conversations) {
        return conversations.stream()
                .map(Conversation::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .map(Enum::name)
                .toList();
    }

    private byte[] exportBody(List<Conversation> conversations, String format) {
        return switch (format) {
            case "CSV" -> jsonlExportService.toCsv(conversations).getBytes(StandardCharsets.UTF_8);
            case "PDF" -> jsonlExportService.toPdfReport(conversations, categoriesIncluded(conversations));
            default -> jsonlExportService.toJsonl(conversations).getBytes(StandardCharsets.UTF_8);
        };
    }

    private String normalizeFormat(String format) {
        String normalized = format == null ? "JSONL" : format.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CSV", "PDF" -> normalized;
            default -> "JSONL";
        };
    }

    private String extension(String format) {
        return switch (format) {
            case "CSV" -> "csv";
            case "PDF" -> "pdf";
            default -> "jsonl";
        };
    }

    private MediaType contentType(String format) {
        return switch (format) {
            case "CSV" -> MediaType.parseMediaType("text/csv");
            case "PDF" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private PreviewRow toPreviewRow(Conversation conversation) {
        return new PreviewRow(
                conversation.getTitle(),
                conversation.getCategory() == null ? "" : conversation.getCategory().name(),
                conversation.getStatus() == null ? "" : conversation.getStatus().name(),
                conversation.isAiCorrected(),
                firstContent(conversation, MessageRole.USER),
                firstContent(conversation, MessageRole.ASSISTANT)
        );
    }

    private String firstContent(Conversation conversation, MessageRole role) {
        return conversation.getTurns().stream()
                .filter(turn -> turn.getRole() == role)
                .map(turn -> previewText(turn.getContent()))
                .findFirst()
                .orElse("");
    }

    private String previewText(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.strip();
        return trimmed.length() > 160 ? trimmed.substring(0, 157) + "..." : trimmed;
    }

    public record PreviewRow(String title,
                             String category,
                             String status,
                             boolean aiCorrected,
                             String userMessage,
                             String assistantMessage) {
    }
}

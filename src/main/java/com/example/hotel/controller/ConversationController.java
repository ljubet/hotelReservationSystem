package com.example.hotel.controller;

import com.example.hotel.dto.ConversationCreateRequest;
import com.example.hotel.dto.ConversationDetailsResponse;
import com.example.hotel.dto.ConversationMessageCreateRequest;
import com.example.hotel.dto.ConversationMessageResponse;
import com.example.hotel.dto.ConversationMessageUpdateRequest;
import com.example.hotel.dto.ConversationResponse;
import com.example.hotel.dto.ConversationUpdateRequest;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<ConversationResponse> getAll() {
        return conversationService.getAll();
    }

    @GetMapping("/{id}")
    public ConversationDetailsResponse getById(@PathVariable Long id) {
        return conversationService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> create(@Valid @RequestBody ConversationCreateRequest request) {
        return new ResponseEntity<>(conversationService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ConversationResponse update(@PathVariable Long id, @Valid @RequestBody ConversationUpdateRequest request) {
        return conversationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ConversationMessageResponse> addMessage(@PathVariable Long id,
                                                                  @Valid @RequestBody ConversationMessageCreateRequest request) {
        return new ResponseEntity<>(conversationService.addMessage(id, request), HttpStatus.CREATED);
    }

    @PutMapping("/{conversationId}/messages/{messageId}")
    public ConversationMessageResponse updateMessage(@PathVariable Long conversationId,
                                                     @PathVariable Long messageId,
                                                     @Valid @RequestBody ConversationMessageUpdateRequest request) {
        return conversationService.updateMessage(conversationId, messageId, request);
    }

    @DeleteMapping("/{conversationId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long conversationId, @PathVariable Long messageId) {
        conversationService.deleteMessage(conversationId, messageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<String> exportConversation(@PathVariable Long id) {
        String payload = conversationService.exportConversationAsJson(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("conversation-" + id + ".json"))
                .body(payload);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportAll() {
        String payload = conversationService.exportAllConversationsAsJson();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("conversations-export.json"))
                .body(payload);
    }

    @GetMapping("/category/{category}")
    public List<ConversationResponse> filterByCategory(@PathVariable ConversationCategory category) {
        return conversationService.filterByCategory(category);
    }

    @GetMapping("/hotel/{hotelId}")
    public List<ConversationResponse> filterByHotel(@PathVariable Long hotelId) {
        return conversationService.filterByHotel(hotelId);
    }

    @GetMapping("/language/{language}")
    public List<ConversationResponse> filterByLanguage(@PathVariable String language) {
        return conversationService.filterByLanguage(language);
    }

    @PostMapping("/generate/{count}")
    public ResponseEntity<Map<String, Integer>> generate(@PathVariable int count) {
        int created = conversationService.generateConversations(count);
        return ResponseEntity.ok(Map.of("generated", created));
    }

    private String contentDisposition(String filename) {
        return "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" +
                java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8);
    }
}

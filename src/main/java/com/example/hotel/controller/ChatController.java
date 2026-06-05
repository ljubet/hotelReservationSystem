package com.example.hotel.controller;

import com.example.hotel.entity.ChatMessage;
import com.example.hotel.repository.ChatMessageRepository;
import com.example.hotel.service.ChatbotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatbotService chatbotService;

    public ChatController(ChatMessageRepository chatMessageRepository, ChatbotService chatbotService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatbotService = chatbotService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(@Valid @RequestBody ChatSendRequest request) {
        String reply = normalizeReply(chatbotService.getReply(
                request.getQuestion(),
                request.getSenderEmail(),
                request.getSenderName()));
        ChatMessage message = new ChatMessage();
        message.setSenderName(request.getSenderName());
        message.setSenderEmail(request.getSenderEmail());
        message.setQuestion(request.getQuestion());
        message.setAnswer(reply);
        boolean escalated = shouldEscalate(reply);
        message.setAnswered(!escalated);
        message.setEscalatedToAdmin(escalated);
        message.setAiAnswered(true);
        message.setAnsweredAt(LocalDateTime.now());
        ChatMessage saved = chatMessageRepository.save(message);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "question", saved.getQuestion(),
                "answer", reply,
                "answered", !escalated));
    }

    @GetMapping("/messages")
    public List<ChatMessage> messages(@RequestParam String email) {
        return chatMessageRepository.findBySenderEmailOrderByCreatedAtAsc(email);
    }

    private String normalizeReply(String reply) {
        if (reply == null || reply.isBlank()) {
            return ChatbotService.UNAVAILABLE_REPLY;
        }
        return reply.trim();
    }

    private boolean shouldEscalate(String reply) {
        if (reply == null || reply.isBlank()) {
            return true;
        }
        String normalized = reply.toLowerCase();
        return ChatbotService.UNAVAILABLE_REPLY.equals(reply)
                || normalized.contains("team will follow up")
                || normalized.contains("staff will follow up");
    }

    @Getter
    @Setter
    public static class ChatSendRequest {
        @NotBlank
        private String senderName;

        @Email
        @NotBlank
        private String senderEmail;

        @NotBlank
        private String question;
    }
}

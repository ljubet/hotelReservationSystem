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

    private static final String FALLBACK = "Thanks for your question! Our team will reply shortly.";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatbotService chatbotService;

    public ChatController(ChatMessageRepository chatMessageRepository, ChatbotService chatbotService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatbotService = chatbotService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(@Valid @RequestBody ChatSendRequest request) {
        String reply = chatbotService.getReply(request.getQuestion());
        ChatMessage message = new ChatMessage();
        message.setSenderName(request.getSenderName());
        message.setSenderEmail(request.getSenderEmail());
        message.setQuestion(request.getQuestion());
        if (reply != null) {
            message.setAnswer(reply);
            message.setAnswered(true);
            message.setAnsweredAt(LocalDateTime.now());
        }
        ChatMessage saved = chatMessageRepository.save(message);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "question", saved.getQuestion(),
                "answer", reply == null ? FALLBACK : reply,
                "answered", reply != null));
    }

    @GetMapping("/messages")
    public List<ChatMessage> messages(@RequestParam String email) {
        return chatMessageRepository.findBySenderEmailOrderByCreatedAtAsc(email);
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

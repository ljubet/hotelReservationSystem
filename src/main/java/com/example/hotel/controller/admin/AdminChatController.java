package com.example.hotel.controller.admin;

import com.example.hotel.entity.ChatMessage;
import com.example.hotel.repository.ChatMessageRepository;
import com.example.hotel.service.ConversationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class AdminChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationService conversationService;

    public AdminChatController(ChatMessageRepository chatMessageRepository,
                               ConversationService conversationService) {
        this.chatMessageRepository = chatMessageRepository;
        this.conversationService = conversationService;
    }

    @GetMapping("/admin/chat")
    public String inbox(Model model) {
        model.addAttribute("messages", chatMessageRepository.findAllByOrderByCreatedAtDesc());
        return "admin/chat/list";
    }

    @GetMapping("/admin/chat/{id}")
    public String replyForm(@PathVariable Long id, Model model) {
        model.addAttribute("message", chatMessageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found")));
        return "admin/chat/detail";
    }

    @PostMapping("/admin/chat/{id}/reply")
    public String reply(@PathVariable Long id,
                        @RequestParam String answer,
                        @RequestParam(defaultValue = "false") boolean saveConversation,
                        RedirectAttributes redirectAttributes) {
        ChatMessage message = chatMessageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        boolean wasAiAnswered = message.isAiAnswered();
        message.setAnswer(answer);
        message.setAnswered(true);
        message.setAiAnswered(false);
        message.setEscalatedToAdmin(false);
        message.setAnsweredAt(LocalDateTime.now());
        if (saveConversation && !message.isSavedAsConversation()) {
            conversationService.createFromChat(message, wasAiAnswered);
            message.setSavedAsConversation(true);
        }
        chatMessageRepository.save(message);
        redirectAttributes.addFlashAttribute("success", "Reply saved.");
        return "redirect:/admin/chat";
    }

    @PostMapping("/admin/chat/{id}/ai-feedback")
    public String aiFeedback(@PathVariable Long id,
                             @RequestParam boolean correct,
                             RedirectAttributes redirectAttributes) {
        ChatMessage message = chatMessageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        message.setAiCorrect(correct);
        chatMessageRepository.save(message);
        redirectAttributes.addFlashAttribute("success", correct ? "AI answer marked correct." : "AI answer marked incorrect.");
        return "redirect:/admin/chat/" + id;
    }
}

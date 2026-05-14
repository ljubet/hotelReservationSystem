package com.example.hotel.controller;

import com.example.hotel.dto.ChatSimulationRequest;
import com.example.hotel.dto.ChatSimulationResponse;
import com.example.hotel.dto.ConversationCreateRequest;
import com.example.hotel.dto.ConversationDetailsResponse;
import com.example.hotel.dto.ConversationMessageCreateRequest;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.MessageRole;
import com.example.hotel.service.ChatbotSimulationService;
import com.example.hotel.service.ConversationService;
import com.example.hotel.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.stream.Collectors;

@Controller
public class ConversationViewController {

    private final ConversationService conversationService;
    private final HotelService hotelService;
    private final ChatbotSimulationService chatbotSimulationService;

    public ConversationViewController(ConversationService conversationService,
                                      HotelService hotelService,
                                      ChatbotSimulationService chatbotSimulationService) {
        this.conversationService = conversationService;
        this.hotelService = hotelService;
        this.chatbotSimulationService = chatbotSimulationService;
    }

    @GetMapping("/conversations")
    public String conversations(Model model) {
        model.addAttribute("pageTitle", "Conversations");
        model.addAttribute("conversations", conversationService.getAll());
        model.addAttribute("categories", ConversationCategory.values());
        model.addAttribute("hotels", hotelService.getAll());
        model.addAttribute("hotelNames", hotelService.getAll().stream()
                .collect(Collectors.toMap(hotel -> hotel.id(), hotel -> hotel.name())));
        model.addAttribute("conversationForm",
                new ConversationCreateRequest("", ConversationCategory.GENERAL_INFORMATION, "English", "", null));
        return "conversations";
    }

    @PostMapping("/conversations")
    public String createConversation(@Valid @ModelAttribute("conversationForm") ConversationCreateRequest request) {
        conversationService.create(request);
        return "redirect:/conversations";
    }

    @PostMapping("/conversations/{id}/delete")
    public String deleteConversation(@PathVariable Long id) {
        conversationService.delete(id);
        return "redirect:/conversations";
    }

    @GetMapping("/conversations/{id}")
    public String conversationDetails(@PathVariable Long id, Model model) {
        ConversationDetailsResponse conversation = conversationService.getById(id);
        model.addAttribute("pageTitle", "Conversation Details");
        model.addAttribute("conversation", conversation);
        model.addAttribute("messages", conversation.messages());
        model.addAttribute("roles", MessageRole.values());
        model.addAttribute("messageForm", new ConversationMessageCreateRequest(MessageRole.USER, "", 1));
        model.addAttribute("hotelNames", hotelService.getAll().stream()
                .collect(Collectors.toMap(hotel -> hotel.id(), hotel -> hotel.name())));
        return "conversation-details";
    }

    @PostMapping("/conversations/{id}/messages")
    public String addMessage(@PathVariable Long id,
                             @Valid @ModelAttribute("messageForm") ConversationMessageCreateRequest request) {
        conversationService.addMessage(id, request);
        return "redirect:/conversations/" + id;
    }

    @PostMapping("/conversations/{conversationId}/messages/{messageId}/delete")
    public String deleteMessage(@PathVariable Long conversationId, @PathVariable Long messageId) {
        conversationService.deleteMessage(conversationId, messageId);
        return "redirect:/conversations/" + conversationId;
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("pageTitle", "Chat Simulation");
        model.addAttribute("chatForm", new ChatSimulationRequest(""));
        return "chat";
    }

    @PostMapping("/chat")
    public String simulate(@Valid @ModelAttribute("chatForm") ChatSimulationRequest request, Model model) {
        ChatSimulationResponse response = chatbotSimulationService.simulate(request);
        model.addAttribute("pageTitle", "Chat Simulation");
        model.addAttribute("chatForm", request);
        model.addAttribute("chatResult", response);
        return "chat";
    }
}

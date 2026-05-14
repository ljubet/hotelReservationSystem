package com.example.hotel.controller;

import com.example.hotel.dto.ChatSimulationRequest;
import com.example.hotel.dto.ChatSimulationResponse;
import com.example.hotel.service.ChatbotSimulationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatSimulationController {

    private final ChatbotSimulationService chatbotSimulationService;

    public ChatSimulationController(ChatbotSimulationService chatbotSimulationService) {
        this.chatbotSimulationService = chatbotSimulationService;
    }

    @PostMapping("/simulate")
    public ChatSimulationResponse simulate(@Valid @RequestBody ChatSimulationRequest request) {
        return chatbotSimulationService.simulate(request);
    }
}


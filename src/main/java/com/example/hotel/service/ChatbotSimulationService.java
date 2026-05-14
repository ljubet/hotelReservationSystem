package com.example.hotel.service;

import com.example.hotel.dto.ChatSimulationRequest;
import com.example.hotel.dto.ChatSimulationResponse;
import com.example.hotel.entity.ConversationCategory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ChatbotSimulationService {

    public ChatSimulationResponse simulate(ChatSimulationRequest request) {
        String message = request.message();
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (normalized.contains("parking")) {
            return build(message, ConversationCategory.PARKING,
                    "Simulation: We offer parking for hotel guests, including accessible spaces. (Rule-based response, not a real LLM.)");
        }
        if (normalized.contains("breakfast")) {
            return build(message, ConversationCategory.BREAKFAST,
                    "Simulation: Breakfast is served daily and can be added to your stay. (Rule-based response, not a real LLM.)");
        }
        if (normalized.contains("check in") || normalized.contains("check-in")) {
            return build(message, ConversationCategory.CHECK_IN,
                    "Simulation: Check-in is available in the afternoon; early check-in depends on availability. (Rule-based response, not a real LLM.)");
        }
        if (normalized.contains("check out") || normalized.contains("check-out")) {
            return build(message, ConversationCategory.CHECK_OUT,
                    "Simulation: Check-out is in the morning; late check-out may be arranged. (Rule-based response, not a real LLM.)");
        }
        if (normalized.contains("cancel") || normalized.contains("cancellation")) {
            return build(message, ConversationCategory.CANCELLATION,
                    "Simulation: Cancellations are accepted up to a set deadline; policies vary by rate. (Rule-based response, not a real LLM.)");
        }
        if (normalized.contains("price") || normalized.contains("cost") || normalized.contains("room")) {
            return build(message, ConversationCategory.RESERVATION,
                    "Simulation: Room pricing depends on dates and availability; we can check options for you. (Rule-based response, not a real LLM.)");
        }
        return build(message, ConversationCategory.GENERAL_INFORMATION,
                "Simulation: I'm here to help with hotel questions and bookings. (Rule-based response, not a real LLM.)");
    }

    private ChatSimulationResponse build(String message, ConversationCategory category, String response) {
        return new ChatSimulationResponse(message, response, category, "RULE_BASED_SIMULATION");
    }
}


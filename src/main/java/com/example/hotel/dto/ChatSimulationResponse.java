package com.example.hotel.dto;

import com.example.hotel.entity.ConversationCategory;

public record ChatSimulationResponse(
        String userMessage,
        String assistantResponse,
        ConversationCategory matchedCategory,
        String source
) {
}


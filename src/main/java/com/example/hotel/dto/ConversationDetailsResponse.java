package com.example.hotel.dto;

import com.example.hotel.entity.ConversationCategory;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationDetailsResponse(
        Long id,
        String title,
        ConversationCategory category,
        String language,
        String description,
        Long hotelId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long messageCount,
        List<ConversationMessageResponse> messages
) {
}


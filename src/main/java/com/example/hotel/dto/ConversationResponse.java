package com.example.hotel.dto;

import com.example.hotel.entity.ConversationCategory;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        String title,
        ConversationCategory category,
        String language,
        String description,
        Long hotelId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long messageCount
) {
}


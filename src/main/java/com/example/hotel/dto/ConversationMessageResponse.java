package com.example.hotel.dto;

import com.example.hotel.entity.MessageRole;

import java.time.LocalDateTime;

public record ConversationMessageResponse(
        Long id,
        Long conversationId,
        MessageRole role,
        String content,
        Integer orderNumber,
        LocalDateTime createdAt
) {
}


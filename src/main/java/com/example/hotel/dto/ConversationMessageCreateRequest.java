package com.example.hotel.dto;

import com.example.hotel.entity.MessageRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConversationMessageCreateRequest(
        @NotNull MessageRole role,
        @NotBlank @Size(max = 5000) String content,
        @NotNull Integer orderNumber
) {
}


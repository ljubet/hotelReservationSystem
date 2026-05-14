package com.example.hotel.dto;

import com.example.hotel.entity.ConversationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConversationUpdateRequest(
        @NotBlank String title,
        @NotNull ConversationCategory category,
        @NotBlank String language,
        @Size(max = 2000) String description,
        Long hotelId
) {
}


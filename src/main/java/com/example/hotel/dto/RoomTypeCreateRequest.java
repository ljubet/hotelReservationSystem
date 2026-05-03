package com.example.hotel.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RoomTypeCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(1) Integer maxGuests,
        @NotNull @Positive BigDecimal basePricePerNight
) {
}


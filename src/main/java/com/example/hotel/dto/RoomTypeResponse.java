package com.example.hotel.dto;

import java.math.BigDecimal;

public record RoomTypeResponse(
        Long id,
        String name,
        String description,
        Integer maxGuests,
        BigDecimal basePricePerNight
) {
}


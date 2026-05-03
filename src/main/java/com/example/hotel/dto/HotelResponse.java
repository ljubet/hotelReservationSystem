package com.example.hotel.dto;

import java.time.LocalDateTime;

public record HotelResponse(
        Long id,
        String name,
        String description,
        String address,
        String city,
        String country,
        String phone,
        String email,
        Integer starRating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


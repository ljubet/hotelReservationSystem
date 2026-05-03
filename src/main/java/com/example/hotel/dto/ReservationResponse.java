package com.example.hotel.dto;

import com.example.hotel.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long guestId,
        Long roomId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer numberOfGuests,
        BigDecimal totalPrice,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


package com.example.hotel.service;

import java.math.BigDecimal;

public record ChatActionPlan(
        String action,
        String roomName,
        String roomType,
        String checkIn,
        String checkOut,
        Long reservationId,
        BigDecimal maxBudget,
        Integer minCapacity
) {

    public static ChatActionPlan none() {
        return new ChatActionPlan("NONE", null, null, null, null, null, null, null);
    }
}

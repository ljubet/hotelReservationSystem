package com.example.hotel.dto;

import com.example.hotel.entity.RoomStatus;

public record RoomResponse(
        Long id,
        String roomNumber,
        Integer floor,
        RoomStatus status,
        Long hotelId,
        Long roomTypeId
) {
}


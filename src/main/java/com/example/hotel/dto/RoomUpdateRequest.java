package com.example.hotel.dto;

import com.example.hotel.entity.RoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomUpdateRequest(
        @NotBlank String roomNumber,
        @NotNull @Min(0) Integer floor,
        @NotNull RoomStatus status,
        @NotNull Long hotelId,
        @NotNull Long roomTypeId
) {
}


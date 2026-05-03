package com.example.hotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record HotelCreateRequest(
        @NotBlank String name,
        String description,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String country,
        @NotBlank String phone,
        @NotBlank @Email String email,
        @Min(1) @Max(5) Integer starRating
) {
}


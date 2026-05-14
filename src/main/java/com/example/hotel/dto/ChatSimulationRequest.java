package com.example.hotel.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatSimulationRequest(
        @NotBlank String message
) {
}


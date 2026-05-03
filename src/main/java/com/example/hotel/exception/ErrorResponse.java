package com.example.hotel.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> validationErrors
) {

    public record FieldViolation(String field, String message) {
    }
}


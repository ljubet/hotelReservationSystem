package com.example.hotel.controller;

import com.example.hotel.dto.ReservationCreateRequest;
import com.example.hotel.dto.ReservationResponse;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public List<ReservationResponse> getAll() {
        return reservationService.getAll();
    }

    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable Long id) {
        return reservationService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request) {
        return new ResponseEntity<>(reservationService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id) {
        return reservationService.cancel(id);
    }

    @PutMapping("/{id}/confirm")
    public ReservationResponse confirm(@PathVariable Long id) {
        return reservationService.confirm(id);
    }

    @GetMapping("/availability")
    public List<RoomResponse> getAvailability(@RequestParam @NotNull Long hotelId,
                                              @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                              LocalDate checkInDate,
                                              @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                              LocalDate checkOutDate,
                                              @RequestParam @NotNull @Min(1) Integer guests) {
        return reservationService.getAvailability(hotelId, checkInDate, checkOutDate, guests);
    }
}


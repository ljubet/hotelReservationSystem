package com.example.hotel.controller;

import com.example.hotel.dto.RoomCreateRequest;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.dto.RoomUpdateRequest;
import com.example.hotel.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/api/rooms")
    public List<RoomResponse> getAll() {
        return roomService.getAll();
    }

    @GetMapping("/api/rooms/{id}")
    public RoomResponse getById(@PathVariable Long id) {
        return roomService.getById(id);
    }

    @GetMapping("/api/hotels/{hotelId}/rooms")
    public List<RoomResponse> getByHotel(@PathVariable Long hotelId) {
        return roomService.getByHotel(hotelId);
    }

    @PostMapping("/api/rooms")
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomCreateRequest request) {
        return new ResponseEntity<>(roomService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/api/rooms/{id}")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomUpdateRequest request) {
        return roomService.update(id, request);
    }

    @DeleteMapping("/api/rooms/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


package com.example.hotel.controller;

import com.example.hotel.dto.RoomTypeCreateRequest;
import com.example.hotel.dto.RoomTypeResponse;
import com.example.hotel.dto.RoomTypeUpdateRequest;
import com.example.hotel.service.RoomTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public List<RoomTypeResponse> getAll() {
        return roomTypeService.getAll();
    }

    @PostMapping
    public ResponseEntity<RoomTypeResponse> create(@Valid @RequestBody RoomTypeCreateRequest request) {
        return new ResponseEntity<>(roomTypeService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public RoomTypeResponse update(@PathVariable Long id, @Valid @RequestBody RoomTypeUpdateRequest request) {
        return roomTypeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


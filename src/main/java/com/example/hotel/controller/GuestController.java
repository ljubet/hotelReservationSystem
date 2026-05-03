package com.example.hotel.controller;

import com.example.hotel.dto.GuestCreateRequest;
import com.example.hotel.dto.GuestResponse;
import com.example.hotel.dto.GuestUpdateRequest;
import com.example.hotel.service.GuestService;
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
@RequestMapping("/api/guests")
public class GuestController {

    private final GuestService guestService;

    public GuestController(GuestService guestService) {
        this.guestService = guestService;
    }

    @GetMapping
    public List<GuestResponse> getAll() {
        return guestService.getAll();
    }

    @GetMapping("/{id}")
    public GuestResponse getById(@PathVariable Long id) {
        return guestService.getById(id);
    }

    @PostMapping
    public ResponseEntity<GuestResponse> create(@Valid @RequestBody GuestCreateRequest request) {
        return new ResponseEntity<>(guestService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public GuestResponse update(@PathVariable Long id, @Valid @RequestBody GuestUpdateRequest request) {
        return guestService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        guestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


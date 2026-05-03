package com.example.hotel.service;

import com.example.hotel.dto.GuestCreateRequest;
import com.example.hotel.dto.GuestResponse;
import com.example.hotel.dto.GuestUpdateRequest;
import com.example.hotel.entity.Guest;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.repository.GuestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuestService {

    private final GuestRepository guestRepository;

    public GuestService(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    public List<GuestResponse> getAll() {
        return guestRepository.findAll().stream().map(this::toResponse).toList();
    }

    public GuestResponse getById(Long id) {
        return toResponse(findGuest(id));
    }

    public GuestResponse create(GuestCreateRequest request) {
        Guest guest = new Guest();
        apply(request, guest);
        return toResponse(guestRepository.save(guest));
    }

    public GuestResponse update(Long id, GuestUpdateRequest request) {
        Guest guest = findGuest(id);
        apply(request, guest);
        return toResponse(guestRepository.save(guest));
    }

    public void delete(Long id) {
        Guest guest = findGuest(id);
        guestRepository.delete(guest);
    }

    private Guest findGuest(Long id) {
        return guestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Guest not found: " + id));
    }

    private void apply(GuestCreateRequest request, Guest guest) {
        guest.setFirstName(request.firstName());
        guest.setLastName(request.lastName());
        guest.setEmail(request.email());
        guest.setPhone(request.phone());
    }

    private void apply(GuestUpdateRequest request, Guest guest) {
        guest.setFirstName(request.firstName());
        guest.setLastName(request.lastName());
        guest.setEmail(request.email());
        guest.setPhone(request.phone());
    }

    private GuestResponse toResponse(Guest guest) {
        return new GuestResponse(
                guest.getId(),
                guest.getFirstName(),
                guest.getLastName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getCreatedAt()
        );
    }
}


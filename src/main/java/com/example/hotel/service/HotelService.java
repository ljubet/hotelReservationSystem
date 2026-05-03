package com.example.hotel.service;

import com.example.hotel.dto.HotelCreateRequest;
import com.example.hotel.dto.HotelResponse;
import com.example.hotel.dto.HotelUpdateRequest;
import com.example.hotel.entity.Hotel;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.repository.HotelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelService {

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    public List<HotelResponse> getAll() {
        return hotelRepository.findAll().stream().map(this::toResponse).toList();
    }

    public HotelResponse getById(Long id) {
        return toResponse(findHotel(id));
    }

    public HotelResponse create(HotelCreateRequest request) {
        Hotel hotel = new Hotel();
        apply(request, hotel);
        return toResponse(hotelRepository.save(hotel));
    }

    public HotelResponse update(Long id, HotelUpdateRequest request) {
        Hotel hotel = findHotel(id);
        apply(request, hotel);
        return toResponse(hotelRepository.save(hotel));
    }

    public void delete(Long id) {
        Hotel hotel = findHotel(id);
        hotelRepository.delete(hotel);
    }

    private Hotel findHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hotel not found: " + id));
    }

    private void apply(HotelCreateRequest request, Hotel hotel) {
        hotel.setName(request.name());
        hotel.setDescription(request.description());
        hotel.setAddress(request.address());
        hotel.setCity(request.city());
        hotel.setCountry(request.country());
        hotel.setPhone(request.phone());
        hotel.setEmail(request.email());
        hotel.setStarRating(request.starRating());
    }

    private void apply(HotelUpdateRequest request, Hotel hotel) {
        hotel.setName(request.name());
        hotel.setDescription(request.description());
        hotel.setAddress(request.address());
        hotel.setCity(request.city());
        hotel.setCountry(request.country());
        hotel.setPhone(request.phone());
        hotel.setEmail(request.email());
        hotel.setStarRating(request.starRating());
    }

    private HotelResponse toResponse(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getDescription(),
                hotel.getAddress(),
                hotel.getCity(),
                hotel.getCountry(),
                hotel.getPhone(),
                hotel.getEmail(),
                hotel.getStarRating(),
                hotel.getCreatedAt(),
                hotel.getUpdatedAt()
        );
    }
}


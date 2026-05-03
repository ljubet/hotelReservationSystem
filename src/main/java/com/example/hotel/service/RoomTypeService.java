package com.example.hotel.service;

import com.example.hotel.dto.RoomTypeCreateRequest;
import com.example.hotel.dto.RoomTypeResponse;
import com.example.hotel.dto.RoomTypeUpdateRequest;
import com.example.hotel.entity.RoomType;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    public List<RoomTypeResponse> getAll() {
        return roomTypeRepository.findAll().stream().map(this::toResponse).toList();
    }

    public RoomTypeResponse create(RoomTypeCreateRequest request) {
        RoomType roomType = new RoomType();
        apply(request, roomType);
        return toResponse(roomTypeRepository.save(roomType));
    }

    public RoomTypeResponse update(Long id, RoomTypeUpdateRequest request) {
        RoomType roomType = findRoomType(id);
        apply(request, roomType);
        return toResponse(roomTypeRepository.save(roomType));
    }

    public void delete(Long id) {
        RoomType roomType = findRoomType(id);
        roomTypeRepository.delete(roomType);
    }

    private RoomType findRoomType(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room type not found: " + id));
    }

    private void apply(RoomTypeCreateRequest request, RoomType roomType) {
        roomType.setName(request.name());
        roomType.setDescription(request.description());
        roomType.setMaxGuests(request.maxGuests());
        roomType.setBasePricePerNight(request.basePricePerNight());
    }

    private void apply(RoomTypeUpdateRequest request, RoomType roomType) {
        roomType.setName(request.name());
        roomType.setDescription(request.description());
        roomType.setMaxGuests(request.maxGuests());
        roomType.setBasePricePerNight(request.basePricePerNight());
    }

    private RoomTypeResponse toResponse(RoomType roomType) {
        return new RoomTypeResponse(
                roomType.getId(),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getMaxGuests(),
                roomType.getBasePricePerNight()
        );
    }
}


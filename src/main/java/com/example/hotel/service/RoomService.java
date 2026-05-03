package com.example.hotel.service;

import com.example.hotel.dto.RoomCreateRequest;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.dto.RoomUpdateRequest;
import com.example.hotel.entity.Hotel;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomStatus;
import com.example.hotel.entity.RoomType;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RoomService(RoomRepository roomRepository,
                       HotelRepository hotelRepository,
                       RoomTypeRepository roomTypeRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    public List<RoomResponse> getAll() {
        return roomRepository.findAll().stream().map(this::toResponse).toList();
    }

    public RoomResponse getById(Long id) {
        return toResponse(findRoom(id));
    }

    public List<RoomResponse> getByHotel(Long hotelId) {
        return roomRepository.findByHotelId(hotelId).stream().map(this::toResponse).toList();
    }

    public RoomResponse create(RoomCreateRequest request) {
        Room room = new Room();
        apply(request, room);
        return toResponse(roomRepository.save(room));
    }

    public RoomResponse update(Long id, RoomUpdateRequest request) {
        Room room = findRoom(id);
        apply(request, room);
        return toResponse(roomRepository.save(room));
    }

    public void delete(Long id) {
        Room room = findRoom(id);
        roomRepository.delete(room);
    }

    public List<Room> getAvailableRooms(Long hotelId, int guests) {
        return roomRepository.findByHotelIdAndStatusAndRoomType_MaxGuestsGreaterThanEqual(
                hotelId,
                RoomStatus.AVAILABLE,
                guests
        );
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room not found: " + id));
    }

    private void apply(RoomCreateRequest request, Room room) {
        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new NotFoundException("Hotel not found: " + request.hotelId()));
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new NotFoundException("Room type not found: " + request.roomTypeId()));
        room.setRoomNumber(request.roomNumber());
        room.setFloor(request.floor());
        room.setStatus(request.status());
        room.setHotel(hotel);
        room.setRoomType(roomType);
    }

    private void apply(RoomUpdateRequest request, Room room) {
        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new NotFoundException("Hotel not found: " + request.hotelId()));
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new NotFoundException("Room type not found: " + request.roomTypeId()));
        room.setRoomNumber(request.roomNumber());
        room.setFloor(request.floor());
        room.setStatus(request.status());
        room.setHotel(hotel);
        room.setRoomType(roomType);
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getStatus(),
                room.getHotel().getId(),
                room.getRoomType().getId()
        );
    }
}


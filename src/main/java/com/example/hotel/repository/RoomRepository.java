package com.example.hotel.repository;

import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    List<Room> findByHotelIdAndStatusAndRoomType_MaxGuestsGreaterThanEqual(Long hotelId, RoomStatus status, Integer maxGuests);
}


package com.example.hotel.repository;

import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findTop3ByAvailableTrueOrderByPricePerNightAsc();

    List<Room> findByAvailableTrueOrderByPricePerNightAsc();

    List<Room> findByRoomTypeAndPricePerNightLessThanEqualAndCapacityGreaterThanEqualOrderByPricePerNightAsc(
            RoomType roomType,
            BigDecimal maxPrice,
            Integer minCapacity);
}

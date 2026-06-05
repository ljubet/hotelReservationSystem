package com.example.hotel.repository;

import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomReview;
import com.example.hotel.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomReviewRepository extends JpaRepository<RoomReview, Long> {

    List<RoomReview> findByRoomOrderByCreatedAtDesc(Room room);

    Optional<RoomReview> findByRoomAndUser(Room room, User user);
}

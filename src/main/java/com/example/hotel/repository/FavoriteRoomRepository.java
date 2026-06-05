package com.example.hotel.repository;

import com.example.hotel.entity.FavoriteRoom;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRoomRepository extends JpaRepository<FavoriteRoom, Long> {

    boolean existsByUserAndRoom(User user, Room room);

    Optional<FavoriteRoom> findByUserAndRoom(User user, Room room);

    List<FavoriteRoom> findByUserOrderByCreatedAtDesc(User user);
}

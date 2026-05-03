package com.example.hotel.repository;

import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByRoomIdAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            Long roomId,
            Collection<ReservationStatus> statuses,
            LocalDate checkOutDate,
            LocalDate checkInDate
    );

    boolean existsByRoomIdAndStatusInAndIdNotAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            Long roomId,
            Collection<ReservationStatus> statuses,
            Long id,
            LocalDate checkOutDate,
            LocalDate checkInDate
    );
}


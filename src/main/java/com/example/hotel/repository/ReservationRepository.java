package com.example.hotel.repository;

import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByStatusAndCheckInLessThanEqualAndCheckOutAfter(ReservationStatus status, LocalDate date, LocalDate sameDate);

    @Query("select r from Reservation r where r.checkIn = :date or r.checkOut = :date order by r.checkIn asc")
    List<Reservation> findTodayActivity(@Param("date") LocalDate date);

    List<Reservation> findByStatusOrderByCreatedAtDesc(ReservationStatus status);

    List<Reservation> findByCheckInBetweenOrderByCreatedAtDesc(LocalDate from, LocalDate to);

    List<Reservation> findByStatusAndCheckInBetweenOrderByCreatedAtDesc(ReservationStatus status, LocalDate from, LocalDate to);

    List<Reservation> findAllByOrderByCreatedAtDesc();

    @Query("""
            select distinct r from Reservation r
            where r.guestEmail = :email
               or r.guestUser.username = :username
            order by r.createdAt desc
            """)
    List<Reservation> findGuestReservations(@Param("username") String username, @Param("email") String email);

    @Query("""
            select count(r) > 0 from Reservation r
            where r.room.id = :roomId
              and r.status in (com.example.hotel.entity.ReservationStatus.PENDING, com.example.hotel.entity.ReservationStatus.CONFIRMED)
              and r.checkIn < :checkOut
              and r.checkOut > :checkIn
            """)
    boolean existsActiveOverlap(@Param("roomId") Long roomId,
                                @Param("checkIn") LocalDate checkIn,
                                @Param("checkOut") LocalDate checkOut);
}

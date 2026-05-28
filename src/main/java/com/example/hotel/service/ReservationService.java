package com.example.hotel.service;

import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.User;
import com.example.hotel.form.ReservationForm;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public Reservation create(ReservationForm form, String username) {
        Room room = roomRepository.findById(form.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
        if (!room.isBookable()) {
            throw new IllegalArgumentException("This room is not available for booking.");
        }
        long nights = ChronoUnit.DAYS.between(form.getCheckIn(), form.getCheckOut());
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }
        if (reservationRepository.existsActiveOverlap(room.getId(), form.getCheckIn(), form.getCheckOut())) {
            throw new IllegalArgumentException("This room is already reserved for the selected dates.");
        }
        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setCheckIn(form.getCheckIn());
        reservation.setCheckOut(form.getCheckOut());
        reservation.setGuestName(form.getGuestName());
        reservation.setGuestEmail(form.getGuestEmail());
        reservation.setTotalPrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            reservation.setGuestUser(user);
        }
        return reservationRepository.save(reservation);
    }
}

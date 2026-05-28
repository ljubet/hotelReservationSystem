package com.example.hotel.service;

import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.User;
import com.example.hotel.form.AdminReservationForm;
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
        validateRoomBookable(room);
        long nights = validateDates(form.getCheckIn(), form.getCheckOut());
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

    public Reservation updateGuestReservation(Long reservationId, ReservationForm form) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending reservations can be edited by guests.");
        }
        Room room = reservation.getRoom();
        validateRoomBookable(room);
        long nights = validateDates(form.getCheckIn(), form.getCheckOut());
        validateNoOverlap(room.getId(), reservation.getId(), form.getCheckIn(), form.getCheckOut());

        reservation.setCheckIn(form.getCheckIn());
        reservation.setCheckOut(form.getCheckOut());
        reservation.setGuestName(form.getGuestName());
        reservation.setGuestEmail(form.getGuestEmail());
        reservation.setTotalPrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
        reservation.setStatus(ReservationStatus.PENDING);
        return reservationRepository.save(reservation);
    }

    public Reservation updateAdminReservation(Long reservationId, AdminReservationForm form) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));
        Room room = roomRepository.findById(form.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
        if (form.getStatus() != ReservationStatus.CANCELLED) {
            validateRoomBookable(room);
            validateNoOverlap(room.getId(), reservation.getId(), form.getCheckIn(), form.getCheckOut());
        }
        long nights = validateDates(form.getCheckIn(), form.getCheckOut());

        reservation.setRoom(room);
        reservation.setCheckIn(form.getCheckIn());
        reservation.setCheckOut(form.getCheckOut());
        reservation.setGuestName(form.getGuestName());
        reservation.setGuestEmail(form.getGuestEmail());
        reservation.setStatus(form.getStatus());
        reservation.setStaffNote(form.getStaffNote());
        reservation.setTotalPrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
        return reservationRepository.save(reservation);
    }

    public ReservationForm toReservationForm(Reservation reservation) {
        ReservationForm form = new ReservationForm();
        form.setRoomId(reservation.getRoom().getId());
        form.setCheckIn(reservation.getCheckIn());
        form.setCheckOut(reservation.getCheckOut());
        form.setGuestName(reservation.getGuestName());
        form.setGuestEmail(reservation.getGuestEmail());
        return form;
    }

    public AdminReservationForm toAdminReservationForm(Reservation reservation) {
        AdminReservationForm form = new AdminReservationForm();
        form.setRoomId(reservation.getRoom().getId());
        form.setCheckIn(reservation.getCheckIn());
        form.setCheckOut(reservation.getCheckOut());
        form.setGuestName(reservation.getGuestName());
        form.setGuestEmail(reservation.getGuestEmail());
        form.setStatus(reservation.getStatus());
        form.setStaffNote(reservation.getStaffNote());
        return form;
    }

    private void validateRoomBookable(Room room) {
        if (!room.isBookable()) {
            throw new IllegalArgumentException("This room is not available for booking.");
        }
    }

    private long validateDates(java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }
        return nights;
    }

    private void validateNoOverlap(Long roomId, Long reservationId, java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        if (reservationRepository.existsActiveOverlapExcludingReservation(roomId, reservationId, checkIn, checkOut)) {
            throw new IllegalArgumentException("This room is already reserved for the selected dates.");
        }
    }
}

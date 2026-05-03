package com.example.hotel.service;

import com.example.hotel.dto.ReservationCreateRequest;
import com.example.hotel.dto.ReservationResponse;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.entity.Guest;
import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomStatus;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.exception.ConflictException;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.repository.GuestRepository;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;

    public ReservationService(ReservationRepository reservationRepository,
                              GuestRepository guestRepository,
                              RoomRepository roomRepository,
                              RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.guestRepository = guestRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    public List<ReservationResponse> getAll() {
        return reservationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ReservationResponse getById(Long id) {
        return toResponse(findReservation(id));
    }

    @Transactional
    public ReservationResponse create(ReservationCreateRequest request) {
        Reservation reservation = new Reservation();
        applyCreate(request, reservation);
        reservation.setStatus(ReservationStatus.PENDING);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse confirm(Long id) {
        Reservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("Only PENDING reservations can be confirmed");
        }
        validateReservation(reservation.getRoom(), reservation.getCheckInDate(),
                reservation.getCheckOutDate(), reservation.getNumberOfGuests(), reservation.getId());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancel(Long id) {
        Reservation reservation = findReservation(id);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return toResponse(reservation);
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.save(reservation));
    }

    public List<RoomResponse> getAvailability(Long hotelId, LocalDate checkInDate, LocalDate checkOutDate, int guests) {
        validateDateRange(checkInDate, checkOutDate);
        List<Room> candidates = roomService.getAvailableRooms(hotelId, guests);
        return candidates.stream()
                .filter(room -> isRoomAvailable(room, checkInDate, checkOutDate, null))
                .map(room -> new RoomResponse(
                        room.getId(),
                        room.getRoomNumber(),
                        room.getFloor(),
                        room.getStatus(),
                        room.getHotel().getId(),
                        room.getRoomType().getId()
                ))
                .toList();
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
    }

    private void applyCreate(ReservationCreateRequest request, Reservation reservation) {
        Guest guest = guestRepository.findById(request.guestId())
                .orElseThrow(() -> new NotFoundException("Guest not found: " + request.guestId()));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new NotFoundException("Room not found: " + request.roomId()));
        validateReservation(room, request.checkInDate(), request.checkOutDate(), request.numberOfGuests(), null);
        reservation.setGuest(guest);
        reservation.setRoom(room);
        reservation.setCheckInDate(request.checkInDate());
        reservation.setCheckOutDate(request.checkOutDate());
        reservation.setNumberOfGuests(request.numberOfGuests());
        reservation.setTotalPrice(calculateTotalPrice(room, request.checkInDate(), request.checkOutDate()));
    }

    private void validateReservation(Room room, LocalDate checkInDate, LocalDate checkOutDate, int guests, Long excludeId) {
        validateDateRange(checkInDate, checkOutDate);
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new ConflictException("Room is not available for booking");
        }
        if (guests > room.getRoomType().getMaxGuests()) {
            throw new BadRequestException("Number of guests exceeds room capacity");
        }
        boolean available = isRoomAvailable(room, checkInDate, checkOutDate, excludeId);
        if (!available) {
            throw new ConflictException("Room has an overlapping reservation");
        }
    }

    private void validateDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            throw new BadRequestException("checkOutDate must be after checkInDate");
        }
    }

    private boolean isRoomAvailable(Room room, LocalDate checkInDate, LocalDate checkOutDate, Long excludeId) {
        if (excludeId == null) {
            return !reservationRepository.existsByRoomIdAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                    room.getId(),
                    EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED),
                    checkOutDate,
                    checkInDate
            );
        }
        return !reservationRepository.existsByRoomIdAndStatusInAndIdNotAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                room.getId(),
                EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED),
                excludeId,
                checkOutDate,
                checkInDate
        );
    }

    private BigDecimal calculateTotalPrice(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights <= 0) {
            throw new BadRequestException("Reservation must be at least one night");
        }
        return room.getRoomType()
                .getBasePricePerNight()
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getGuest().getId(),
                reservation.getRoom().getId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getNumberOfGuests(),
                reservation.getTotalPrice(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}


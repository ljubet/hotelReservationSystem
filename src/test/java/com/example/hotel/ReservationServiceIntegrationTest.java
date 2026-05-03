package com.example.hotel;

import com.example.hotel.dto.ReservationCreateRequest;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.entity.Guest;
import com.example.hotel.entity.Hotel;
import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomStatus;
import com.example.hotel.entity.RoomType;
import com.example.hotel.repository.GuestRepository;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.RoomTypeRepository;
import com.example.hotel.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void createsAndBlocksOverlappingReservations() {
        Hotel hotel = new Hotel();
        hotel.setName("City Hotel");
        hotel.setDescription("Central location");
        hotel.setAddress("99 Main St");
        hotel.setCity("Metro");
        hotel.setCountry("USA");
        hotel.setPhone("+1-555-0111");
        hotel.setEmail("hello@city.example");
        hotel.setStarRating(3);
        hotelRepository.save(hotel);

        RoomType roomType = new RoomType();
        roomType.setName("Deluxe");
        roomType.setDescription("Deluxe room");
        roomType.setMaxGuests(2);
        roomType.setBasePricePerNight(new BigDecimal("150.00"));
        roomTypeRepository.save(roomType);

        Room room = new Room();
        room.setRoomNumber("303");
        room.setFloor(3);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setHotel(hotel);
        room.setRoomType(roomType);
        roomRepository.save(room);

        Guest guest = new Guest();
        guest.setFirstName("Alex");
        guest.setLastName("Kim");
        guest.setEmail("alex.kim@example.com");
        guest.setPhone("+1-555-0122");
        guestRepository.save(guest);

        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(2);

        ReservationCreateRequest request = new ReservationCreateRequest(
                guest.getId(),
                room.getId(),
                checkIn,
                checkOut,
                2
        );
        var reservation = reservationService.create(request);
        assertThat(reservation.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);

        List<RoomResponse> available = reservationService.getAvailability(
                hotel.getId(),
                checkIn,
                checkOut,
                2
        );
        assertThat(available).isEmpty();

        reservationService.cancel(reservation.id());
        reservationRepository.flush();

        List<RoomResponse> afterCancel = reservationService.getAvailability(
                hotel.getId(),
                checkIn,
                checkOut,
                2
        );
        assertThat(afterCancel).hasSize(1);
    }
}


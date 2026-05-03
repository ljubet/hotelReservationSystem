package com.example.hotel.config;

import com.example.hotel.entity.Guest;
import com.example.hotel.entity.Hotel;
import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomStatus;
import com.example.hotel.entity.RoomType;
import com.example.hotel.repository.GuestRepository;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.RoomTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;

    public DataSeeder(HotelRepository hotelRepository,
                      RoomTypeRepository roomTypeRepository,
                      RoomRepository roomRepository,
                      GuestRepository guestRepository,
                      ReservationRepository reservationRepository) {
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public void run(String... args) {
        if (hotelRepository.count() > 0) {
            return;
        }

        Hotel hotel = new Hotel();
        hotel.setName("Riverside Hotel");
        hotel.setDescription("Modern hotel near the river.");
        hotel.setAddress("123 River Road");
        hotel.setCity("Springfield");
        hotel.setCountry("USA");
        hotel.setPhone("+1-555-0100");
        hotel.setEmail("info@riverside.example");
        hotel.setStarRating(4);
        hotelRepository.save(hotel);

        RoomType standard = new RoomType();
        standard.setName("Standard");
        standard.setDescription("Standard room with queen bed");
        standard.setMaxGuests(2);
        standard.setBasePricePerNight(new BigDecimal("129.00"));
        roomTypeRepository.save(standard);

        RoomType suite = new RoomType();
        suite.setName("Suite");
        suite.setDescription("Suite with living area and king bed");
        suite.setMaxGuests(4);
        suite.setBasePricePerNight(new BigDecimal("249.00"));
        roomTypeRepository.save(suite);

        Room room101 = new Room();
        room101.setRoomNumber("101");
        room101.setFloor(1);
        room101.setStatus(RoomStatus.AVAILABLE);
        room101.setHotel(hotel);
        room101.setRoomType(standard);
        roomRepository.save(room101);

        Room room201 = new Room();
        room201.setRoomNumber("201");
        room201.setFloor(2);
        room201.setStatus(RoomStatus.AVAILABLE);
        room201.setHotel(hotel);
        room201.setRoomType(suite);
        roomRepository.save(room201);

        Guest guest = new Guest();
        guest.setFirstName("Jamie");
        guest.setLastName("Lee");
        guest.setEmail("jamie.lee@example.com");
        guest.setPhone("+1-555-0199");
        guestRepository.save(guest);

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoom(room101);
        reservation.setCheckInDate(LocalDate.now().plusDays(3));
        reservation.setCheckOutDate(LocalDate.now().plusDays(5));
        reservation.setNumberOfGuests(2);
        reservation.setTotalPrice(new BigDecimal("258.00"));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
    }
}


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
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final int HOTEL_COUNT = 10;
    private static final int ROOM_TYPES_COUNT = 6;
    private static final int ROOMS_PER_HOTEL = 30;
    private static final int GUEST_COUNT = 150;
    private static final int RESERVATION_COUNT = 250;
    private static final int ROOMS_PER_FLOOR = 5;

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
        resetData();

        Faker faker = new Faker(new Locale("en"), new Random(42));
        List<RoomType> roomTypes = seedRoomTypes(faker);
        List<Hotel> hotels = seedHotels(faker);
        List<Room> rooms = seedRooms(faker, hotels, roomTypes);
        List<Guest> guests = seedGuests(faker);
        seedReservations(faker, rooms, guests);
    }

    private void resetData() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        guestRepository.deleteAll();
        roomTypeRepository.deleteAll();
        hotelRepository.deleteAll();
    }

    private List<RoomType> seedRoomTypes(Faker faker) {
        List<RoomType> roomTypes = new ArrayList<>();
        String[][] defaults = {
                {"Standard", "Standard room with queen bed", "2", "129.00"},
                {"Deluxe", "Deluxe room with king bed", "2", "169.00"},
                {"Suite", "Suite with living area", "4", "249.00"},
                {"Family", "Family room with two beds", "5", "219.00"},
                {"Executive", "Executive room with workspace", "2", "189.00"},
                {"Penthouse", "Top floor premium suite", "4", "349.00"}
        };

        for (int i = 0; i < ROOM_TYPES_COUNT; i++) {
            String[] source = defaults[i % defaults.length];
            RoomType roomType = new RoomType();
            roomType.setName(source[0]);
            roomType.setDescription(source[1]);
            roomType.setMaxGuests(Integer.parseInt(source[2]));
            roomType.setBasePricePerNight(new BigDecimal(source[3]));
            roomTypes.add(roomTypeRepository.save(roomType));
        }

        return roomTypes;
    }

    private List<Hotel> seedHotels(Faker faker) {
        List<Hotel> hotels = new ArrayList<>();
        for (int i = 0; i < HOTEL_COUNT; i++) {
            String companyName = faker.company().name();
            Hotel hotel = new Hotel();
            hotel.setName(companyName + " Hotel");
            hotel.setDescription(faker.company().catchPhrase());
            hotel.setAddress(faker.address().streetAddress());
            hotel.setCity(faker.address().city());
            hotel.setCountry(faker.address().country());
            hotel.setPhone(faker.phoneNumber().phoneNumber());
            hotel.setEmail("info@" + slugify(companyName) + ".example");
            hotel.setStarRating(3 + faker.random().nextInt(3));
            hotels.add(hotelRepository.save(hotel));
        }
        return hotels;
    }

    private List<Room> seedRooms(Faker faker, List<Hotel> hotels, List<RoomType> roomTypes) {
        List<Room> rooms = new ArrayList<>();
        for (Hotel hotel : hotels) {
            for (int i = 0; i < ROOMS_PER_HOTEL; i++) {
                int floor = (i / ROOMS_PER_FLOOR) + 1;
                int roomIndex = (i % ROOMS_PER_FLOOR) + 1;
                Room room = new Room();
                room.setRoomNumber(String.valueOf((floor * 100) + roomIndex));
                room.setFloor(floor);
                room.setStatus(randomRoomStatus(faker));
                room.setHotel(hotel);
                room.setRoomType(roomTypes.get(faker.random().nextInt(roomTypes.size())));
                rooms.add(roomRepository.save(room));
            }
        }
        return rooms;
    }

    private List<Guest> seedGuests(Faker faker) {
        List<Guest> guests = new ArrayList<>();
        for (int i = 0; i < GUEST_COUNT; i++) {
            Guest guest = new Guest();
            guest.setFirstName(faker.name().firstName());
            guest.setLastName(faker.name().lastName());
            guest.setEmail("guest" + (i + 1) + "@example.com");
            guest.setPhone(faker.phoneNumber().phoneNumber());
            guests.add(guestRepository.save(guest));
        }
        return guests;
    }

    private void seedReservations(Faker faker, List<Room> rooms, List<Guest> guests) {
        List<Room> availableRooms = rooms.stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .toList();

        if (availableRooms.isEmpty()) {
            return;
        }

        Map<Long, LocalDate> nextStartDate = new HashMap<>();
        LocalDate now = LocalDate.now();
        for (Room room : availableRooms) {
            nextStartDate.put(room.getId(), now.minusDays(120));
        }

        for (int i = 0; i < RESERVATION_COUNT; i++) {
            Room room = availableRooms.get(faker.random().nextInt(availableRooms.size()));
            LocalDate start = nextStartDate.get(room.getId()).plusDays(faker.random().nextInt(14) + 1);
            int nights = faker.random().nextInt(7) + 1;
            LocalDate end = start.plusDays(nights);

            RoomType roomType = room.getRoomType();
            int maxGuests = roomType.getMaxGuests();
            int guestsCount = faker.random().nextInt(maxGuests) + 1;

            Reservation reservation = new Reservation();
            reservation.setGuest(guests.get(faker.random().nextInt(guests.size())));
            reservation.setRoom(room);
            reservation.setCheckInDate(start);
            reservation.setCheckOutDate(end);
            reservation.setNumberOfGuests(guestsCount);
            reservation.setTotalPrice(roomType.getBasePricePerNight()
                    .multiply(BigDecimal.valueOf(nights)));
            reservation.setStatus(resolveStatus(now, end, faker));
            reservationRepository.save(reservation);

            nextStartDate.put(room.getId(), end.plusDays(faker.random().nextInt(10) + 1));
        }
    }

    private RoomStatus randomRoomStatus(Faker faker) {
        int roll = faker.random().nextInt(100);
        if (roll < 80) {
            return RoomStatus.AVAILABLE;
        }
        if (roll < 95) {
            return RoomStatus.MAINTENANCE;
        }
        return RoomStatus.OUT_OF_SERVICE;
    }

    private ReservationStatus resolveStatus(LocalDate now, LocalDate checkOutDate, Faker faker) {
        if (checkOutDate.isBefore(now)) {
            return ReservationStatus.COMPLETED;
        }
        int roll = faker.random().nextInt(100);
        if (roll < 10) {
            return ReservationStatus.CANCELLED;
        }
        if (roll < 55) {
            return ReservationStatus.CONFIRMED;
        }
        return ReservationStatus.PENDING;
    }

    private String slugify(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}

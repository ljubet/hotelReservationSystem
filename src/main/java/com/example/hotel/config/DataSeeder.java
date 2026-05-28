package com.example.hotel.config;

import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.ConversationStatus;
import com.example.hotel.entity.ConversationTurn;
import com.example.hotel.entity.MessageRole;
import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomType;
import com.example.hotel.entity.User;
import com.example.hotel.entity.UserRole;
import com.example.hotel.repository.ConversationRepository;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final ConversationRepository conversationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      RoomRepository roomRepository,
                      ReservationRepository reservationRepository,
                      ConversationRepository conversationRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.conversationRepository = conversationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            seedUsers();
        }
        if (roomRepository.count() == 0) {
            seedRooms();
        }
        normalizeRooms();
        if (reservationRepository.count() == 0 && roomRepository.count() > 0) {
            seedReservations();
        }
        if (conversationRepository.count() == 0) {
            seedConversations();
        }
    }

    private void seedUsers() {
        userRepository.save(user("admin", "admin@hotel.test", "admin", UserRole.ADMIN));
        userRepository.save(user("manager", "manager@hotel.test", "admin", UserRole.ADMIN));
        userRepository.save(user("guest", "guest@hotel.test", "guest", UserRole.GUEST));
    }

    private User user(String username, String email, String password, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return user;
    }

    private void seedRooms() {
        roomRepository.save(room("Garden Single", RoomType.SINGLE, "A quiet single room overlooking the courtyard.", "119.00", "4.6", 1,
                "https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1200&q=80", true, false,
                "WiFi,Desk,Courtyard view,Tea station"));
        roomRepository.save(room("Classic Double", RoomType.DOUBLE, "Warm double room with a queen bed and generous natural light.", "169.00", "4.8", 2,
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80", true, false,
                "WiFi,Queen bed,Smart TV,Mini fridge"));
        roomRepository.save(room("Executive Double", RoomType.DOUBLE, "Spacious double room with a work area and city view.", "219.00", "4.7", 2,
                "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80", true, false,
                "WiFi,Workspace,City view,Espresso machine"));
        roomRepository.save(room("Family Suite", RoomType.SUITE, "Two-room suite designed for families and longer stays.", "319.00", "4.9", 4,
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80", true, false,
                "WiFi,Sofa bed,Kitchenette,Bathrobes"));
        roomRepository.save(room("Terrace Suite", RoomType.SUITE, "Premium suite with a private terrace and lounge area.", "429.00", "5.0", 3,
                "https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=1200&q=80", true, false,
                "WiFi,Terrace,Lounge,Evening turndown"));
        roomRepository.save(room("Atrium Double", RoomType.DOUBLE, "A refreshed double room currently held for renovation work.", "149.00", "4.4", 2,
                "https://images.unsplash.com/photo-1598928636135-d146006ff4be?auto=format&fit=crop&w=1200&q=80", true, true,
                "WiFi,Queen bed,Walk-in shower"));
    }

    private Room room(String name, RoomType type, String description, String price, String rating, int capacity, String imageUrl,
                      boolean available, boolean underRenovation, String amenities) {
        Room room = new Room();
        room.setName(name);
        room.setRoomType(type);
        room.setDescription(description);
        room.setPricePerNight(new BigDecimal(price));
        room.setRating(new BigDecimal(rating));
        room.setCapacity(capacity);
        room.setImageUrl(imageUrl);
        room.setAvailable(available);
        room.setUnderRenovation(underRenovation);
        room.setAmenities(amenities);
        return room;
    }

    private void normalizeRooms() {
        for (Room room : roomRepository.findAll()) {
            boolean changed = false;
            if (room.getRating() == null) {
                room.setRating(new BigDecimal("4.7"));
                changed = true;
            }
            if (room.isUnderRenovation() && room.isAvailable()) {
                room.setAvailable(false);
                changed = true;
            }
            if (changed) {
                roomRepository.save(room);
            }
        }
    }

    private void seedReservations() {
        User guest = userRepository.findByUsername("guest").orElse(null);
        Room room = roomRepository.findAll().get(0);
        Reservation reservation = new Reservation();
        reservation.setGuestUser(guest);
        reservation.setRoom(room);
        reservation.setCheckIn(LocalDate.now());
        reservation.setCheckOut(LocalDate.now().plusDays(2));
        reservation.setGuestName("Sample Guest");
        reservation.setGuestEmail("guest@hotel.test");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setTotalPrice(room.getPricePerNight().multiply(BigDecimal.valueOf(2)));
        reservationRepository.save(reservation);
    }

    private void seedConversations() {
        List<ConversationSeed> seeds = List.of(
                new ConversationSeed("Booking dates", ConversationCategory.BOOKING, ConversationStatus.APPROVED, "Do you have double rooms next weekend?", "Yes, please choose a double room on the rooms page and submit your dates."),
                new ConversationSeed("Suite amenities", ConversationCategory.AMENITIES, ConversationStatus.APPROVED, "Does the suite include a kitchenette?", "Our Family Suite includes a kitchenette, lounge area, WiFi, and bathrobes."),
                new ConversationSeed("Late check-out", ConversationCategory.GENERAL, ConversationStatus.APPROVED, "Can I check out late?", "Late check-out may be available depending on occupancy. Please ask reception the night before."),
                new ConversationSeed("Parking availability", ConversationCategory.AMENITIES, ConversationStatus.APPROVED, "Is parking available?", "Yes, on-site parking is available for overnight hotel guests."),
                new ConversationSeed("Room service hours", ConversationCategory.ROOM_SERVICE, ConversationStatus.APPROVED, "Can I order dinner to my room?", "Room service is available for dinner, and our restaurant team can deliver to your room."),
                new ConversationSeed("Noise complaint", ConversationCategory.COMPLAINTS, ConversationStatus.DRAFT, "The room next door is very loud.", "We are sorry for the disturbance. Our front desk can contact the room or help arrange a quieter space."),
                new ConversationSeed("Pet policy", ConversationCategory.GENERAL, ConversationStatus.APPROVED, "Can I bring a small dog?", "Small pets are welcome in selected rooms when noted during booking."),
                new ConversationSeed("Breakfast timing", ConversationCategory.AMENITIES, ConversationStatus.APPROVED, "What time is breakfast?", "Breakfast is served from 6:30 AM to 10:30 AM."),
                new ConversationSeed("Cancellation policy", ConversationCategory.BOOKING, ConversationStatus.APPROVED, "How do I cancel a reservation?", "Flexible reservations can usually be cancelled up to 24 hours before arrival."),
                new ConversationSeed("WiFi access", ConversationCategory.AMENITIES, ConversationStatus.APPROVED, "Do rooms have internet?", "Complimentary high-speed WiFi is available in all rooms and public areas.")
        );
        for (ConversationSeed seed : seeds) {
            Conversation conversation = new Conversation();
            conversation.setTitle(seed.title());
            conversation.setCategory(seed.category());
            conversation.setStatus(seed.status());
            ConversationTurn userTurn = turn(MessageRole.USER, seed.userMessage());
            ConversationTurn assistantTurn = turn(MessageRole.ASSISTANT, seed.assistantMessage());
            conversation.replaceTurns(List.of(userTurn, assistantTurn));
            conversationRepository.save(conversation);
        }
    }

    private ConversationTurn turn(MessageRole role, String content) {
        ConversationTurn turn = new ConversationTurn();
        turn.setRole(role);
        turn.setContent(content);
        return turn;
    }

    private record ConversationSeed(String title, ConversationCategory category, ConversationStatus status,
                                    String userMessage, String assistantMessage) {
    }
}

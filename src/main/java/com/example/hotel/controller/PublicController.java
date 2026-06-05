package com.example.hotel.controller;

import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomType;
import com.example.hotel.entity.RoomReview;
import com.example.hotel.entity.User;
import com.example.hotel.form.ReservationForm;
import com.example.hotel.repository.FavoriteRoomRepository;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomReviewRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class PublicController {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final FavoriteRoomRepository favoriteRoomRepository;
    private final RoomReviewRepository roomReviewRepository;
    private final ReservationRepository reservationRepository;

    public PublicController(RoomRepository roomRepository,
                            UserRepository userRepository,
                            FavoriteRoomRepository favoriteRoomRepository,
                            RoomReviewRepository roomReviewRepository,
                            ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.favoriteRoomRepository = favoriteRoomRepository;
        this.roomReviewRepository = roomReviewRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredRooms", roomRepository.findByAvailableTrueOrderByPricePerNightAsc());
        return "index";
    }

    @GetMapping("/rooms")
    public String rooms(@RequestParam(required = false) RoomType roomType,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(required = false) Integer minCapacity,
                        @RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "20") Integer size,
                        Authentication authentication,
                        Model model) {
        int pageSize = normalizePageSize(size);
        List<Room> filteredRooms = roomRepository.findAll().stream()
                .filter(room -> roomType == null || room.getRoomType() == roomType)
                .filter(room -> maxPrice == null || room.getPricePerNight().compareTo(maxPrice) <= 0)
                .filter(room -> minCapacity == null || room.getCapacity() >= minCapacity)
                .sorted((first, second) -> first.getPricePerNight().compareTo(second.getPricePerNight()))
                .toList();
        int totalRooms = filteredRooms.size();
        int totalPages = (int) Math.ceil((double) totalRooms / pageSize);
        int currentPage = Math.min(Math.max(page, 1), Math.max(totalPages, 1));
        int startIndex = totalRooms == 0 ? 0 : (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalRooms);
        List<Room> pagedRooms = totalRooms == 0 ? List.of() : filteredRooms.subList(startIndex, endIndex);

        model.addAttribute("rooms", pagedRooms);
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("selectedRoomType", roomType);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minCapacity", minCapacity);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageSizes", List.of(12, 20, 40));
        model.addAttribute("startItem", totalRooms == 0 ? 0 : startIndex + 1);
        model.addAttribute("endItem", endIndex);
        model.addAttribute("favoriteRoomIds", favoriteRoomIds(authentication));
        return "rooms/list";
    }

    @GetMapping("/rooms/{id}")
    public String roomDetail(@PathVariable Long id, Authentication authentication, Model model) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        model.addAttribute("room", room);
        model.addAttribute("amenities", splitAmenities(room));
        model.addAttribute("favorite", currentUser(authentication)
                .map(user -> favoriteRoomRepository.existsByUserAndRoom(user, room))
                .orElse(false));
        model.addAttribute("reviews", roomReviewRepository.findByRoomOrderByCreatedAtDesc(room));
        model.addAttribute("availabilityDays", availabilityDays(room));
        return "rooms/detail";
    }

    @PostMapping("/rooms/{id}/reviews")
    public String review(@PathVariable Long id,
                         @RequestParam int rating,
                         @RequestParam(required = false) String comment,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        RoomReview review = roomReviewRepository.findByRoomAndUser(room, user).orElseGet(RoomReview::new);
        review.setRoom(room);
        review.setUser(user);
        review.setRating(Math.max(1, Math.min(5, rating)));
        review.setComment(comment);
        roomReviewRepository.save(review);
        updateRoomRating(room);
        redirectAttributes.addFlashAttribute("success", "Review saved.");
        return "redirect:/rooms/" + id;
    }

    @GetMapping("/reservations/new")
    public String newReservation(@RequestParam Long roomId, Authentication authentication, Model model) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        ReservationForm form = new ReservationForm();
        form.setRoomId(roomId);
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
                form.setGuestName(user.getUsername());
                form.setGuestEmail(user.getEmail());
            });
        }
        model.addAttribute("reservationForm", form);
        model.addAttribute("room", room);
        model.addAttribute("pageTitle", "Reservation Request");
        model.addAttribute("submitLabel", "Submit Reservation");
        return "reservations/form";
    }

    private List<String> splitAmenities(Room room) {
        if (room.getAmenities() == null || room.getAmenities().isBlank()) {
            return List.of();
        }
        return Arrays.stream(room.getAmenities().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private int normalizePageSize(Integer size) {
        if (size == null) {
            return 20;
        }
        return switch (size) {
            case 12, 20, 40 -> size;
            default -> 20;
        };
    }

    private Set<Long> favoriteRoomIds(Authentication authentication) {
        return currentUser(authentication)
                .map(user -> favoriteRoomRepository.findByUserOrderByCreatedAtDesc(user).stream()
                        .map(favorite -> favorite.getRoom().getId())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private Optional<User> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }

    private List<AvailabilityDay> availabilityDays(Room room) {
        LocalDate start = LocalDate.now();
        return start.datesUntil(start.plusDays(30))
                .map(date -> new AvailabilityDay(date, room.isBookable()
                        && !reservationRepository.existsActiveOverlap(room.getId(), date, date.plusDays(1))))
                .toList();
    }

    private void updateRoomRating(Room room) {
        List<RoomReview> reviews = roomReviewRepository.findByRoomOrderByCreatedAtDesc(room);
        if (reviews.isEmpty()) {
            return;
        }
        BigDecimal average = BigDecimal.valueOf(reviews.stream()
                        .mapToInt(RoomReview::getRating)
                        .average()
                        .orElse(4.7))
                .setScale(1, java.math.RoundingMode.HALF_UP);
        room.setRating(average);
        roomRepository.save(room);
    }

    public record AvailabilityDay(LocalDate date, boolean available) {
    }
}

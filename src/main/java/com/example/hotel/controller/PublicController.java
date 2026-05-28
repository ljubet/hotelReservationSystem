package com.example.hotel.controller;

import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomType;
import com.example.hotel.form.ReservationForm;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Controller
public class PublicController {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public PublicController(RoomRepository roomRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
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
        return "rooms/list";
    }

    @GetMapping("/rooms/{id}")
    public String roomDetail(@PathVariable Long id, Model model) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        model.addAttribute("room", room);
        model.addAttribute("amenities", splitAmenities(room));
        return "rooms/detail";
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
}

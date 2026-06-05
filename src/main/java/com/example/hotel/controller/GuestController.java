package com.example.hotel.controller;

import com.example.hotel.entity.FavoriteRoom;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.User;
import com.example.hotel.repository.FavoriteRoomRepository;
import com.example.hotel.repository.ReservationRepository;
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

@Controller
public class GuestController {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final FavoriteRoomRepository favoriteRoomRepository;

    public GuestController(UserRepository userRepository,
                           RoomRepository roomRepository,
                           ReservationRepository reservationRepository,
                           FavoriteRoomRepository favoriteRoomRepository) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.favoriteRoomRepository = favoriteRoomRepository;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("reservations", reservationRepository.findGuestReservations(user.getUsername(), user.getEmail()));
        model.addAttribute("favorites", favoriteRoomRepository.findByUserOrderByCreatedAtDesc(user));
        return "profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@RequestParam(required = false) String fullName,
                              @RequestParam(required = false) String email,
                              @RequestParam(required = false) String preferences,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Email cannot be blank.");
            return "redirect:/profile";
        }
        boolean emailBelongsToAnotherUser = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .isPresent();
        if (emailBelongsToAnotherUser) {
            redirectAttributes.addFlashAttribute("error", "Email is already used by another account.");
            return "redirect:/profile";
        }
        user.setFullName(fullName == null ? null : fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPreferences(preferences);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Profile updated.");
        return "redirect:/profile";
    }

    @PostMapping("/rooms/{id}/favorite")
    public String favorite(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        if (!favoriteRoomRepository.existsByUserAndRoom(user, room)) {
            FavoriteRoom favorite = new FavoriteRoom();
            favorite.setUser(user);
            favorite.setRoom(room);
            favoriteRoomRepository.save(favorite);
        }
        redirectAttributes.addFlashAttribute("success", "Room saved to favorites.");
        return "redirect:/rooms/" + id;
    }

    @PostMapping("/rooms/{id}/favorite/remove")
    public String removeFavorite(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        favoriteRoomRepository.findByUserAndRoom(user, room).ifPresent(favoriteRoomRepository::delete);
        redirectAttributes.addFlashAttribute("success", "Room removed from favorites.");
        return "redirect:/rooms/" + id;
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new EntityNotFoundException("User not found");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}

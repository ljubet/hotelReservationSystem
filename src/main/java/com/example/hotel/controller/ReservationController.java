package com.example.hotel.controller;

import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.Room;
import com.example.hotel.form.ReservationForm;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.UserRepository;
import com.example.hotel.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public ReservationController(ReservationService reservationService,
                                 ReservationRepository reservationRepository,
                                 RoomRepository roomRepository,
                                 UserRepository userRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/reservations/new")
    public String create(@Valid @ModelAttribute ReservationForm reservationForm,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Room room = roomRepository.findById(reservationForm.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
        if (bindingResult.hasErrors()) {
            model.addAttribute("room", room);
            return "reservations/form";
        }
        try {
            String username = authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName()) ? authentication.getName() : null;
            Reservation reservation = reservationService.create(reservationForm, username);
            redirectAttributes.addFlashAttribute("success", "Reservation request submitted.");
            return "redirect:/reservations/confirmation/" + reservation.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("reservation", ex.getMessage());
            model.addAttribute("room", room);
            return "reservations/form";
        }
    }

    @GetMapping("/reservations/confirmation/{id}")
    public String confirmation(@PathVariable Long id, Model model) {
        model.addAttribute("reservation", reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found")));
        return "reservations/confirmation";
    }

    @GetMapping("/reservations/my")
    public String myReservations(Authentication authentication, Model model) {
        String username = authentication.getName();
        String email = userRepository.findByUsername(username)
                .map(user -> user.getEmail())
                .orElse("");
        model.addAttribute("reservations", reservationRepository.findGuestReservations(username, email));
        return "reservations/my";
    }
}

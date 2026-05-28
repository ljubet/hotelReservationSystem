package com.example.hotel.controller;

import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.entity.Room;
import com.example.hotel.form.ReservationForm;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.UserRepository;
import com.example.hotel.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
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
    public String create(@Valid @ModelAttribute("reservationForm") ReservationForm reservationForm,
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

    @GetMapping("/reservations/{id}/edit")
    public String edit(@PathVariable Long id,
                       Authentication authentication,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        Reservation reservation = findOwnedReservation(id, authentication);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            redirectAttributes.addFlashAttribute("error", "Only pending reservations can be edited. Confirmed reservations are locked after hotel approval.");
            return "redirect:/reservations/my";
        }
        model.addAttribute("reservationForm", reservationService.toReservationForm(reservation));
        model.addAttribute("room", reservation.getRoom());
        model.addAttribute("reservationId", reservation.getId());
        model.addAttribute("pageTitle", "Edit Reservation");
        model.addAttribute("submitLabel", "Save Changes");
        return "reservations/form";
    }

    @PostMapping("/reservations/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("reservationForm") ReservationForm reservationForm,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Reservation reservation = findOwnedReservation(id, authentication);
        Room room = reservation.getRoom();
        reservationForm.setRoomId(room.getId());
        if (bindingResult.hasErrors()) {
            model.addAttribute("room", room);
            model.addAttribute("reservationId", id);
            model.addAttribute("pageTitle", "Edit Reservation");
            model.addAttribute("submitLabel", "Save Changes");
            return "reservations/form";
        }
        try {
            reservationService.updateGuestReservation(id, reservationForm);
            redirectAttributes.addFlashAttribute("success", "Reservation updated. The hotel will review the updated request.");
            return "redirect:/reservations/my";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("reservation", ex.getMessage());
            model.addAttribute("room", room);
            model.addAttribute("reservationId", id);
            model.addAttribute("pageTitle", "Edit Reservation");
            model.addAttribute("submitLabel", "Save Changes");
            return "reservations/form";
        }
    }

    private Reservation findOwnedReservation(Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("You must be signed in to edit a reservation.");
        }
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));
        String username = authentication.getName();
        String email = userRepository.findByUsername(username)
                .map(user -> user.getEmail())
                .orElse("");
        boolean ownerByUser = reservation.getGuestUser() != null && username.equals(reservation.getGuestUser().getUsername());
        boolean ownerByEmail = reservation.getGuestEmail() != null && reservation.getGuestEmail().equalsIgnoreCase(email);
        if (!ownerByUser && !ownerByEmail) {
            throw new AccessDeniedException("You can only edit your own reservations.");
        }
        return reservation;
    }
}

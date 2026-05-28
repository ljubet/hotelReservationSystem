package com.example.hotel.controller.admin;

import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AdminReservationController {

    private final ReservationRepository reservationRepository;

    public AdminReservationController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/admin/reservations")
    public String reservations(@RequestParam(required = false) ReservationStatus status,
                               @RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to,
                               Model model) {
        model.addAttribute("reservations", filter(status, from, to));
        model.addAttribute("statuses", ReservationStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/reservations/list";
    }

    @GetMapping("/admin/reservations/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("reservation", reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found")));
        return "admin/reservations/detail";
    }

    @PostMapping("/admin/reservations/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
        redirectAttributes.addFlashAttribute("success", "Reservation confirmed.");
        return "redirect:/admin/reservations/" + id;
    }

    @PostMapping("/admin/reservations/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        redirectAttributes.addFlashAttribute("success", "Reservation cancelled.");
        return "redirect:/admin/reservations/" + id;
    }

    @PostMapping("/admin/reservations/{id}/note")
    public String saveNote(@PathVariable Long id,
                           @RequestParam(required = false) String staffNote,
                           RedirectAttributes redirectAttributes) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));
        reservation.setStaffNote(staffNote);
        reservationRepository.save(reservation);
        redirectAttributes.addFlashAttribute("success", "Staff note saved.");
        return "redirect:/admin/reservations/" + id;
    }

    private List<Reservation> filter(ReservationStatus status, LocalDate from, LocalDate to) {
        if (from != null && to != null && status != null) {
            return reservationRepository.findByStatusAndCheckInBetweenOrderByCreatedAtDesc(status, from, to);
        }
        if (from != null && to != null) {
            return reservationRepository.findByCheckInBetweenOrderByCreatedAtDesc(from, to);
        }
        if (status != null) {
            return reservationRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return reservationRepository.findAllByOrderByCreatedAtDesc();
    }
}

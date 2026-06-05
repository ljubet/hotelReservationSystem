package com.example.hotel.controller.admin;

import com.example.hotel.entity.Reservation;
import com.example.hotel.repository.ReservationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AdminCalendarController {

    private final ReservationRepository reservationRepository;

    public AdminCalendarController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/admin/calendar")
    public String calendar(@RequestParam(required = false) LocalDate from,
                           @RequestParam(required = false) LocalDate to,
                           Model model) {
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start.plusDays(30) : to;
        List<CalendarDay> days = start.datesUntil(end.plusDays(1))
                .map(date -> new CalendarDay(date, reservationsForDate(date)))
                .toList();
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("days", days);
        return "admin/calendar";
    }

    private List<Reservation> reservationsForDate(LocalDate date) {
        return reservationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(reservation -> reservation.getCheckIn().isBefore(date.plusDays(1))
                        && reservation.getCheckOut().isAfter(date))
                .toList();
    }

    public record CalendarDay(LocalDate date, List<Reservation> reservations) {
    }
}

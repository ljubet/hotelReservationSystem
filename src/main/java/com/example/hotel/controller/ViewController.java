package com.example.hotel.controller;

import com.example.hotel.dto.GuestCreateRequest;
import com.example.hotel.dto.HotelCreateRequest;
import com.example.hotel.dto.ReservationCreateRequest;
import com.example.hotel.dto.RoomCreateRequest;
import com.example.hotel.dto.RoomTypeCreateRequest;
import com.example.hotel.entity.RoomStatus;
import com.example.hotel.service.GuestService;
import com.example.hotel.service.HotelService;
import com.example.hotel.service.ReservationService;
import com.example.hotel.service.RoomService;
import com.example.hotel.service.RoomTypeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class ViewController {

    private final HotelService hotelService;
    private final RoomTypeService roomTypeService;
    private final RoomService roomService;
    private final GuestService guestService;
    private final ReservationService reservationService;

    public ViewController(HotelService hotelService,
                          RoomTypeService roomTypeService,
                          RoomService roomService,
                          GuestService guestService,
                          ReservationService reservationService) {
        this.hotelService = hotelService;
        this.roomTypeService = roomTypeService;
        this.roomService = roomService;
        this.guestService = guestService;
        this.reservationService = reservationService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "Hotel Management");
        return "index";
    }

    @GetMapping("/hotels")
    public String hotels(Model model) {
        model.addAttribute("pageTitle", "Hotels");
        model.addAttribute("hotels", hotelService.getAll());
        model.addAttribute("hotelForm", new HotelCreateRequest(null, null, null, null, null, null, null, null));
        return "hotels";
    }

    @GetMapping("/room-types")
    public String roomTypes(Model model) {
        model.addAttribute("pageTitle", "Room Types");
        model.addAttribute("roomTypes", roomTypeService.getAll());
        model.addAttribute("roomTypeForm", new RoomTypeCreateRequest(null, null, null, null));
        return "room-types";
    }

    @GetMapping("/rooms")
    public String rooms(Model model) {
        model.addAttribute("pageTitle", "Rooms");
        model.addAttribute("rooms", roomService.getAll());
        model.addAttribute("hotels", hotelService.getAll());
        model.addAttribute("roomTypes", roomTypeService.getAll());
        model.addAttribute("roomStatuses", RoomStatus.values());
        model.addAttribute("roomForm", new RoomCreateRequest(null, null, null, null, null));
        return "rooms";
    }

    @GetMapping("/guests")
    public String guests(Model model) {
        model.addAttribute("pageTitle", "Guests");
        model.addAttribute("guests", guestService.getAll());
        model.addAttribute("guestForm", new GuestCreateRequest(null, null, null, null));
        return "guests";
    }

    @GetMapping("/reservations")
    public String reservations(Model model) {
        model.addAttribute("pageTitle", "Reservations");
        model.addAttribute("reservations", reservationService.getAll());
        model.addAttribute("guests", guestService.getAll());
        model.addAttribute("rooms", roomService.getAll());
        model.addAttribute("reservationForm", new ReservationCreateRequest(null, null, null, null, null));
        return "reservations";
    }

    @GetMapping("/availability")
    public String availability(Model model,
                               @RequestParam(required = false) Long hotelId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                               LocalDate checkInDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                               LocalDate checkOutDate,
                               @RequestParam(required = false) Integer guests) {
        model.addAttribute("pageTitle", "Availability");
        model.addAttribute("hotels", hotelService.getAll());
        if (hotelId != null && checkInDate != null && checkOutDate != null && guests != null) {
            model.addAttribute("availability",
                    reservationService.getAvailability(hotelId, checkInDate, checkOutDate, guests));
        } else {
            model.addAttribute("availability", List.of());
        }
        model.addAttribute("selectedHotelId", hotelId);
        model.addAttribute("selectedCheckIn", checkInDate);
        model.addAttribute("selectedCheckOut", checkOutDate);
        model.addAttribute("selectedGuests", guests);
        return "availability";
    }
}

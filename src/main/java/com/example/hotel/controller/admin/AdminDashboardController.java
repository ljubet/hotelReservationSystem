package com.example.hotel.controller.admin;

import com.example.hotel.entity.HousekeepingStatus;
import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.entity.Room;
import com.example.hotel.repository.ChatMessageRepository;
import com.example.hotel.repository.ConversationRepository;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class AdminDashboardController {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;

    public AdminDashboardController(RoomRepository roomRepository,
                                    ReservationRepository reservationRepository,
                                    ChatMessageRepository chatMessageRepository,
                                    ConversationRepository conversationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        long totalRooms = roomRepository.count();
        long bookedRooms = reservationRepository.countByStatusAndCheckInLessThanEqualAndCheckOutAfter(ReservationStatus.CONFIRMED, today, today);
        long availableRooms = roomRepository.findAll().stream()
                .filter(Room::isBookable)
                .filter(room -> !reservationRepository.existsActiveOverlap(room.getId(), today, today.plusDays(1)))
                .count();
        long dirtyRooms = roomRepository.findAll().stream()
                .filter(room -> room.getHousekeepingStatus() == HousekeepingStatus.DIRTY)
                .count();
        long outOfServiceRooms = roomRepository.findAll().stream()
                .filter(room -> room.getHousekeepingStatus() == HousekeepingStatus.OUT_OF_SERVICE)
                .count();
        int occupancyPercentage = totalRooms == 0 ? 0 : (int) Math.round((bookedRooms * 100.0) / totalRooms);
        model.addAttribute("totalRooms", roomRepository.count());
        model.addAttribute("activeReservationsToday", bookedRooms);
        model.addAttribute("bookedRooms", bookedRooms);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("occupancyPercentage", occupancyPercentage);
        model.addAttribute("dirtyRooms", dirtyRooms);
        model.addAttribute("outOfServiceRooms", outOfServiceRooms);
        model.addAttribute("unansweredMessages", chatMessageRepository.countByAnsweredFalse());
        model.addAttribute("totalConversations", conversationRepository.count());
        model.addAttribute("todayReservations", reservationRepository.findTodayActivity(today));
        model.addAttribute("recentMessages", chatMessageRepository.findTop5ByAnsweredFalseOrderByCreatedAtDesc());
        return "admin/dashboard";
    }
}

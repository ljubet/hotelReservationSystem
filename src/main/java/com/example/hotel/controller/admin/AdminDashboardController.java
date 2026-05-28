package com.example.hotel.controller.admin;

import com.example.hotel.entity.ReservationStatus;
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
        model.addAttribute("totalRooms", roomRepository.count());
        model.addAttribute("activeReservationsToday",
                reservationRepository.countByStatusAndCheckInLessThanEqualAndCheckOutAfter(ReservationStatus.CONFIRMED, today, today));
        model.addAttribute("unansweredMessages", chatMessageRepository.countByAnsweredFalse());
        model.addAttribute("totalConversations", conversationRepository.count());
        model.addAttribute("todayReservations", reservationRepository.findTodayActivity(today));
        model.addAttribute("recentMessages", chatMessageRepository.findTop5ByAnsweredFalseOrderByCreatedAtDesc());
        return "admin/dashboard";
    }
}

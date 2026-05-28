package com.example.hotel.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ChatbotService {

    public String getReply(String question) {
        if (question == null) {
            return null;
        }
        String input = question.toLowerCase(Locale.ROOT);
        if (containsAny(input, "hotel name", "name of the hotel", "what is this hotel", "which hotel")) {
            return "You are chatting with Aurora Hotel. We can help with rooms, reservations, amenities, and guest services.";
        }
        if (containsAny(input, "check-in", "checkin", "check in")) {
            return "Check-in starts at 3:00 PM. Early check-in is sometimes available; please ask the front desk on arrival day.";
        }
        if (containsAny(input, "check-out", "checkout", "check out")) {
            return "Check-out is at 11:00 AM. Late check-out may be available depending on occupancy.";
        }
        if (input.contains("parking")) {
            return "On-site parking is available for hotel guests, including overnight parking near the main entrance.";
        }
        if (containsAny(input, "pool", "swim")) {
            return "Our pool is open daily from 7:00 AM to 10:00 PM. Towels are available in the pool area.";
        }
        if (containsAny(input, "gym", "fitness", "workout")) {
            return "Our fitness studio includes cardio equipment, weights, towels, and chilled water for hotel guests.";
        }
        if (containsAny(input, "wifi", "internet")) {
            return "Complimentary high-speed WiFi is available throughout the hotel. You will receive access details at check-in.";
        }
        if (containsAny(input, "breakfast", "restaurant", "food")) {
            return "Breakfast is served from 6:30 AM to 10:30 AM, and our restaurant offers lunch, dinner, and room service.";
        }
        if (containsAny(input, "cancel", "cancellation")) {
            return "Most flexible reservations can be cancelled up to 24 hours before arrival without a fee. Prepaid rates may differ.";
        }
        if (containsAny(input, "pet", "dog", "cat")) {
            return "We welcome small pets in selected rooms. Please mention your pet when booking so we can prepare the right room.";
        }
        if (containsAny(input, "price", "cost", "rate", "how much")) {
            return "Room rates vary by room type and date. Please visit the rooms page to compare current prices and availability.";
        }
        if (containsAny(input, "reservation", "book", "available")) {
            return "You can start a reservation from any room detail page. Choose your dates and submit the booking request.";
        }
        return null;
    }

    private boolean containsAny(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

package com.example.hotel.service;

import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomType;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class HotelMcpTools {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public HotelMcpTools(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    @Tool(description = "Returns bookable hotel rooms with name, type, price per night, and capacity.")
    public String getRoomList() {
        List<Room> rooms = roomRepository.findAll().stream()
                .filter(Room::isBookable)
                .sorted(Comparator.comparing(Room::getPricePerNight))
                .toList();
        if (rooms.isEmpty()) {
            return "No rooms are currently available for booking.";
        }
        StringBuilder builder = new StringBuilder("Available rooms:\n");
        for (Room room : rooms) {
            builder.append("- ")
                    .append(room.getName())
                    .append(" (")
                    .append(room.getRoomType())
                    .append("): $")
                    .append(room.getPricePerNight())
                    .append(" per night, capacity ")
                    .append(room.getCapacity())
                    .append('\n');
        }
        return builder.toString();
    }

    @Tool(description = "Returns full details for a room by room name, including amenities and booking status.")
    public String getRoomDetails(@ToolParam(description = "Room name to search for") String roomName) {
        if (roomName == null || roomName.isBlank()) {
            return "Please provide a room name.";
        }
        Optional<Room> room = roomRepository.findAll().stream()
                .filter(candidate -> candidate.getName().equalsIgnoreCase(roomName.trim()))
                .findFirst()
                .or(() -> roomRepository.findAll().stream()
                        .filter(candidate -> candidate.getName().toLowerCase(Locale.ROOT)
                                .contains(roomName.trim().toLowerCase(Locale.ROOT)))
                        .findFirst());
        if (room.isEmpty()) {
            return "No room was found with the name \"" + roomName + "\".";
        }
        Room found = room.get();
        return """
                Room: %s
                Type: %s
                Price per night: $%s
                Capacity: %d guest(s)
                Rating: %s
                Amenities: %s
                Available: %s
                Under renovation: %s
                Description: %s
                """.formatted(
                found.getName(),
                found.getRoomType(),
                found.getPricePerNight(),
                found.getCapacity(),
                found.getRating(),
                blankToDefault(found.getAmenities(), "No amenities listed"),
                found.isBookable() ? "yes" : "no",
                found.isUnderRenovation() ? "yes" : "no",
                found.getDescription());
    }

    @Tool(description = "Checks if any room of the requested type is available for the given ISO dates.")
    public String checkAvailability(
            @ToolParam(description = "Room type: SINGLE, DOUBLE, or SUITE") String roomType,
            @ToolParam(description = "Check-in date in ISO format, for example 2026-06-10") String checkIn,
            @ToolParam(description = "Check-out date in ISO format, for example 2026-06-12") String checkOut) {
        RoomType parsedRoomType;
        LocalDate parsedCheckIn;
        LocalDate parsedCheckOut;
        try {
            parsedRoomType = RoomType.valueOf(roomType.trim().toUpperCase(Locale.ROOT));
            parsedCheckIn = LocalDate.parse(checkIn);
            parsedCheckOut = LocalDate.parse(checkOut);
        } catch (IllegalArgumentException | NullPointerException | DateTimeParseException ex) {
            return "Please provide a valid room type and dates. Room type must be SINGLE, DOUBLE, or SUITE, and dates must use YYYY-MM-DD.";
        }
        if (!parsedCheckOut.isAfter(parsedCheckIn)) {
            return "Check-out must be after check-in.";
        }
        List<Room> availableRooms = roomRepository.findAll().stream()
                .filter(Room::isBookable)
                .filter(room -> room.getRoomType() == parsedRoomType)
                .filter(room -> !reservationRepository.existsActiveOverlap(room.getId(), parsedCheckIn, parsedCheckOut))
                .sorted(Comparator.comparing(Room::getPricePerNight))
                .toList();
        if (availableRooms.isEmpty()) {
            return "No " + parsedRoomType + " rooms are available from " + parsedCheckIn + " to " + parsedCheckOut + ".";
        }
        StringBuilder builder = new StringBuilder("Available ")
                .append(parsedRoomType)
                .append(" rooms from ")
                .append(parsedCheckIn)
                .append(" to ")
                .append(parsedCheckOut)
                .append(":\n");
        for (Room room : availableRooms) {
            builder.append("- ")
                    .append(room.getName())
                    .append(": $")
                    .append(room.getPricePerNight())
                    .append(" per night, capacity ")
                    .append(room.getCapacity())
                    .append('\n');
        }
        return builder.toString();
    }

    @Tool(description = "Returns Aurora Hotel policies, address, check-in, check-out, parking, pool, WiFi, pets, and cancellation information.")
    public String getHotelInfo() {
        return """
                Aurora Hotel information:
                Address: 18 Aurora Avenue, City Center.
                Check-in: starts at 3:00 PM.
                Check-out: 11:00 AM.
                Parking: on-site overnight guest parking is available near the main entrance.
                Pool: open daily from 7:00 AM to 10:00 PM.
                WiFi: complimentary high-speed WiFi is available in rooms and public areas.
                Fitness: fitness studio with cardio equipment, weights, towels, and chilled water.
                Pets: small pets are welcome in selected rooms when mentioned during booking.
                Dining: breakfast is served from 6:30 AM to 10:30 AM, with restaurant and room service options.
                Cancellation: most flexible reservations can be cancelled up to 24 hours before arrival without a fee; prepaid rates may differ.
                """;
    }

    @Tool(description = "Looks up reservation status by guest email and returns booking status, room, and dates.")
    public String getReservationStatus(@ToolParam(description = "Guest email address") String email) {
        if (email == null || email.isBlank()) {
            return "Please provide the guest email address used for the reservation.";
        }
        List<Reservation> reservations = reservationRepository.findByGuestEmailOrderByCreatedAtDesc(email.trim());
        if (reservations.isEmpty()) {
            return "No reservations were found for " + email + ".";
        }
        StringBuilder builder = new StringBuilder("Reservations for ").append(email).append(":\n");
        for (Reservation reservation : reservations) {
            builder.append("- ")
                    .append(reservation.getRoom().getName())
                    .append(": ")
                    .append(reservation.getStatus())
                    .append(", ")
                    .append(reservation.getCheckIn())
                    .append(" to ")
                    .append(reservation.getCheckOut())
                    .append(", total $")
                    .append(reservation.getTotalPrice())
                    .append('\n');
        }
        return builder.toString();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

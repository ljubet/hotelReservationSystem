package com.example.hotel.service;

import com.example.hotel.entity.Reservation;
import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomType;
import com.example.hotel.entity.User;
import com.example.hotel.form.ReservationForm;
import com.example.hotel.repository.ReservationRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ChronoField;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatActionService {

    private static final Pattern ISO_DATE = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
    private static final Pattern NUMERIC_DATE = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?\\b");
    private static final Pattern MONTH_DATE = Pattern.compile(
            "\\b(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+\\d{1,2}(?:,?\\s*\\d{4})?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RESERVATION_ID = Pattern.compile("\\b(?:reservation|booking)\\s*#?\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern STANDALONE_ID = Pattern.compile("^#?(\\d+)$");
    private static final Pattern MONEY = Pattern.compile("(?:\\$|budget\\s+|under\\s+|below\\s+|max\\s+)(\\d{2,5})(?:\\.\\d{1,2})?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPACITY = Pattern.compile("\\b(?:for\\s+)?(\\d+)\\s*(?:people|guests|persons|person)\\b", Pattern.CASE_INSENSITIVE);
    private static final List<DateTimeFormatter> MONTH_DAY_YEAR_FORMATTERS = List.of(
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d yyyy").toFormatter(Locale.ENGLISH)
    );

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final ConcurrentMap<String, ChatActionState> states = new ConcurrentHashMap<>();

    public ChatActionService(RoomRepository roomRepository,
                             ReservationRepository reservationRepository,
                             UserRepository userRepository,
                             ReservationService reservationService) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.reservationService = reservationService;
    }

    @Transactional
    public Optional<String> handle(String question, String senderName, String senderEmail) {
        if (question == null || question.isBlank() || senderEmail == null || senderEmail.isBlank()) {
            return Optional.empty();
        }
        if (asksForRoomCount(question)) {
            long totalRooms = roomRepository.count();
            long bookableRooms = roomRepository.findAll().stream().filter(Room::isBookable).count();
            return Optional.of("Aurora Hotel currently has " + totalRooms + " rooms in the system, with "
                    + bookableRooms + " currently bookable.");
        }
        String key = senderEmail.trim().toLowerCase(Locale.ROOT);
        ChatActionState state = states.get(key);
        if (state != null) {
            return Optional.of(continueAction(key, state, question, senderName, senderEmail));
        }

        Action action = detectAction(question);
        if (action == Action.NONE) {
            return Optional.empty();
        }

        ChatActionState newState = new ChatActionState(action);
        newState.guestName = senderName;
        newState.guestEmail = senderEmail;
        absorbDetails(newState, question);
        return Optional.of(advance(key, newState));
    }

    private String continueAction(String key, ChatActionState state, String question, String senderName, String senderEmail) {
        if (isAbort(question)) {
            states.remove(key);
            return "No problem. I cancelled that chat action.";
        }
        state.guestName = blankToDefault(state.guestName, senderName);
        state.guestEmail = blankToDefault(state.guestEmail, senderEmail);

        if (state.awaitingConfirmation) {
            if (isPositiveConfirmation(question)) {
                states.remove(key);
                return performConfirmedAction(state);
            }
            if (isNegativeConfirmation(question)) {
                states.remove(key);
                return "No problem. I did not make any changes.";
            }
            return "Please reply yes to confirm, or no to cancel this action.";
        }

        absorbDetails(state, question);
        return advance(key, state);
    }

    private Action detectAction(String question) {
        String text = normalize(question);
        boolean mentionsReservation = containsAny(text, "reservation", "booking");
        boolean wantsCancel = containsAny(text, "cancel", "delete");
        boolean wantsChange = containsAny(text, "change", "edit", "modify", "update", "move", "extend");

        if (containsAny(text, "recommend", "suggest") || (containsAny(text, "budget") && containsAny(text, "room"))) {
            return Action.RECOMMEND_ROOM;
        }
        if (mentionsReservation && wantsCancel) {
            return Action.CANCEL_RESERVATION;
        }
        if ((mentionsReservation && wantsChange)
                || containsAny(text, "change reservation", "edit reservation", "modify reservation", "change booking", "edit booking")) {
            return Action.CHANGE_RESERVATION;
        }
        if (containsAny(text, "book", "reserve", "make a reservation", "make reservation", "create reservation")) {
            return Action.CREATE_RESERVATION;
        }
        if (containsAny(text, "available", "availability", "free room", "open room")) {
            return Action.CHECK_AVAILABILITY;
        }
        if (containsAny(text, "price", "cost", "how much", "total")) {
            return Action.PRICE_QUOTE;
        }
        return Action.NONE;
    }

    private String advance(String key, ChatActionState state) {
        return switch (state.action) {
            case CREATE_RESERVATION -> advanceCreateReservation(key, state);
            case CANCEL_RESERVATION -> advanceCancelReservation(key, state);
            case CHANGE_RESERVATION -> advanceChangeReservation(key, state);
            case CHECK_AVAILABILITY -> advanceAvailability(key, state);
            case PRICE_QUOTE -> advancePriceQuote(key, state);
            case RECOMMEND_ROOM -> advanceRecommendation(key, state);
            case NONE -> "";
        };
    }

    private String advanceCreateReservation(String key, ChatActionState state) {
        if (state.roomId == null && state.roomType == null) {
            states.put(key, state);
            return "Sure. What room type would you like: single, double, or suite? You can also tell me a specific room name.";
        }
        if (state.checkIn == null || state.checkOut == null) {
            states.put(key, state);
            return "What check-in and check-out dates should I use? You can write them like June 7 to June 15, 7/6 to 15/6, or 2026-06-07 to 2026-06-15.";
        }
        String dateError = validateDateText(state.checkIn, state.checkOut);
        if (dateError != null) {
            states.remove(key);
            return dateError;
        }
        Optional<Room> room = resolveAvailableRoom(state, null);
        if (room.isEmpty()) {
            states.remove(key);
            return "I could not find an available matching room for those dates. Try a different room type or different dates.";
        }
        state.roomId = room.get().getId();
        state.awaitingConfirmation = true;
        states.put(key, state);
        return "I found " + room.get().getName() + " from " + state.checkIn + " to " + state.checkOut
                + ". The estimated total is " + money(totalPrice(room.get(), state.checkIn, state.checkOut))
                + ". Reply yes to submit this as a pending reservation request, or no to cancel.";
    }

    private String advanceCancelReservation(String key, ChatActionState state) {
        Optional<Reservation> reservation = resolvePendingReservationForGuest(state);
        if (reservation.isEmpty()) {
            List<Reservation> pending = pendingReservations(state.guestEmail);
            if (pending.isEmpty()) {
                boolean hasConfirmed = reservationsForGuest(state.guestEmail).stream()
                        .anyMatch(reservationItem -> reservationItem.getStatus() == ReservationStatus.CONFIRMED);
                states.remove(key);
                return hasConfirmed
                        ? "You have confirmed reservation(s), but I cannot cancel confirmed stays automatically. Please contact the hotel team."
                        : "I could not find a pending reservation for your email.";
            }
            states.put(key, state);
            return "Which pending reservation should I cancel? Please reply with the reservation ID:\n" + formatReservations(pending);
        }
        state.reservationId = reservation.get().getId();
        state.awaitingConfirmation = true;
        states.put(key, state);
        return "Please confirm: cancel reservation #" + reservation.get().getId() + " for "
                + reservation.get().getRoom().getName() + " from " + reservation.get().getCheckIn()
                + " to " + reservation.get().getCheckOut() + "? Reply yes or no.";
    }

    private String advanceChangeReservation(String key, ChatActionState state) {
        Optional<Reservation> reservation = resolvePendingReservationForGuest(state);
        if (reservation.isEmpty()) {
            List<Reservation> pending = pendingReservations(state.guestEmail);
            if (pending.isEmpty()) {
                states.remove(key);
                return "I could not find a pending reservation that can be changed. Confirmed reservations are locked after hotel approval.";
            }
            states.put(key, state);
            return "Which pending reservation should I change? Please reply with the reservation ID:\n" + formatReservations(pending);
        }
        state.reservationId = reservation.get().getId();
        if (state.checkIn == null) {
            state.checkIn = reservation.get().getCheckIn();
        }
        if (state.checkOut == null) {
            state.checkOut = reservation.get().getCheckOut();
        }
        if (state.checkIn == null || state.checkOut == null) {
            states.put(key, state);
            return "What new check-in and check-out dates should I use? You can write them like June 7 to June 15, 7/6 to 15/6, or 2026-06-07 to 2026-06-15.";
        }
        String dateError = validateDateText(state.checkIn, state.checkOut);
        if (dateError != null) {
            states.remove(key);
            return dateError;
        }
        Reservation found = reservation.get();
        if (reservationRepository.existsActiveOverlapExcludingReservation(
                found.getRoom().getId(), found.getId(), state.checkIn, state.checkOut)) {
            states.remove(key);
            return found.getRoom().getName() + " is not available for those new dates.";
        }
        state.awaitingConfirmation = true;
        states.put(key, state);
        return "I can change reservation #" + found.getId() + " for " + found.getRoom().getName()
                + " to " + state.checkIn + " through " + state.checkOut
                + ". The new estimated total is " + money(totalPrice(found.getRoom(), state.checkIn, state.checkOut))
                + ". Reply yes to save this change, or no to cancel.";
    }

    private String advanceAvailability(String key, ChatActionState state) {
        if (state.roomId == null && state.roomType == null) {
            states.put(key, state);
            return "Which room type should I check: single, double, or suite?";
        }
        if (state.checkIn == null || state.checkOut == null) {
            states.put(key, state);
            return "What dates should I check? You can write them like June 7 to June 15, 7/6 to 15/6, or 2026-06-07 to 2026-06-15.";
        }
        String dateError = validateDateText(state.checkIn, state.checkOut);
        if (dateError != null) {
            states.remove(key);
            return dateError;
        }
        states.remove(key);
        if (state.roomId != null) {
            Room room = roomRepository.findById(state.roomId).orElse(null);
            if (room == null || !room.isBookable()) {
                return "That room is not currently bookable.";
            }
            boolean available = !reservationRepository.existsActiveOverlap(room.getId(), state.checkIn, state.checkOut);
            return available
                    ? room.getName() + " is available from " + state.checkIn + " to " + state.checkOut
                    + ". Estimated total: " + money(totalPrice(room, state.checkIn, state.checkOut)) + "."
                    : room.getName() + " is not available from " + state.checkIn + " to " + state.checkOut + ".";
        }
        List<Room> rooms = availableRoomsByType(state.roomType, state.checkIn, state.checkOut);
        if (rooms.isEmpty()) {
            return "No " + state.roomType + " rooms are available from " + state.checkIn + " to " + state.checkOut + ".";
        }
        StringBuilder builder = new StringBuilder("Available ")
                .append(state.roomType)
                .append(" rooms from ")
                .append(state.checkIn)
                .append(" to ")
                .append(state.checkOut)
                .append(":\n");
        rooms.stream().limit(5).forEach(room -> builder.append("- ")
                .append(room.getName())
                .append(": ")
                .append(money(room.getPricePerNight()))
                .append(" per night, total ")
                .append(money(totalPrice(room, state.checkIn, state.checkOut)))
                .append('\n'));
        return builder.toString().trim();
    }

    private String advancePriceQuote(String key, ChatActionState state) {
        if (state.roomId == null && state.roomType == null) {
            states.put(key, state);
            return "Which room or room type should I price: single, double, or suite?";
        }
        if (state.checkIn == null || state.checkOut == null) {
            states.put(key, state);
            return "For a reservation total, send the check-in and check-out dates, for example June 7 to June 15 or 2026-06-07 to 2026-06-15.";
        }
        String dateError = validateDateText(state.checkIn, state.checkOut);
        if (dateError != null) {
            states.remove(key);
            return dateError;
        }
        Optional<Room> room = resolveAvailableRoom(state, null);
        states.remove(key);
        if (room.isEmpty()) {
            return "I could not find an available matching room for those dates, so I cannot calculate a reservation total.";
        }
        return room.get().getName() + " is " + money(room.get().getPricePerNight()) + " per night. From "
                + state.checkIn + " to " + state.checkOut + ", the estimated total is "
                + money(totalPrice(room.get(), state.checkIn, state.checkOut)) + ".";
    }

    private String advanceRecommendation(String key, ChatActionState state) {
        if (state.maxBudget == null) {
            states.put(key, state);
            return "What maximum nightly budget should I use?";
        }
        if (state.minCapacity == null) {
            states.put(key, state);
            return "How many guests should the room fit?";
        }
        states.remove(key);
        List<Room> matches = roomRepository.findAll().stream()
                .filter(Room::isBookable)
                .filter(room -> state.roomType == null || room.getRoomType() == state.roomType)
                .filter(room -> room.getPricePerNight().compareTo(state.maxBudget) <= 0)
                .filter(room -> room.getCapacity() >= state.minCapacity)
                .sorted(Comparator.comparing(Room::getRating).reversed()
                        .thenComparing(Room::getPricePerNight))
                .limit(5)
                .toList();
        if (matches.isEmpty()) {
            return "I could not find a room within " + money(state.maxBudget)
                    + " per night for " + state.minCapacity + " guest(s). Try a higher budget or fewer guests.";
        }
        StringBuilder builder = new StringBuilder("Here are good room matches for ")
                .append(state.minCapacity)
                .append(" guest(s) under ")
                .append(money(state.maxBudget))
                .append(" per night:\n");
        for (Room room : matches) {
            builder.append("- ")
                    .append(room.getName())
                    .append(" (")
                    .append(room.getRoomType())
                    .append("): ")
                    .append(money(room.getPricePerNight()))
                    .append(" per night, capacity ")
                    .append(room.getCapacity())
                    .append(", rating ")
                    .append(room.getRating())
                    .append("/5\n");
        }
        return builder.toString().trim();
    }

    private String performConfirmedAction(ChatActionState state) {
        return switch (state.action) {
            case CREATE_RESERVATION -> createReservation(state);
            case CANCEL_RESERVATION -> cancelReservation(state);
            case CHANGE_RESERVATION -> changeReservation(state);
            default -> "There is nothing to confirm for that request.";
        };
    }

    private String createReservation(ChatActionState state) {
        ReservationForm form = new ReservationForm();
        form.setRoomId(state.roomId);
        form.setCheckIn(state.checkIn);
        form.setCheckOut(state.checkOut);
        form.setGuestName(blankToDefault(state.guestName, "Guest"));
        form.setGuestEmail(state.guestEmail);
        String username = userRepository.findByEmail(state.guestEmail)
                .map(User::getUsername)
                .orElse(null);
        try {
            Reservation reservation = reservationService.create(form, username);
            return "Done. I submitted reservation #" + reservation.getId() + " for "
                    + reservation.getRoom().getName() + ". It is pending hotel approval.";
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
    }

    private String cancelReservation(ChatActionState state) {
        Reservation reservation = reservationRepository.findById(state.reservationId).orElse(null);
        if (reservation == null || !ownsReservation(reservation, state.guestEmail)) {
            return "I could not find that reservation for your email.";
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            return "Only pending reservations can be cancelled by the chatbot. Please contact the hotel team for confirmed reservations.";
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        return "Reservation #" + reservation.getId() + " has been cancelled.";
    }

    private String changeReservation(ChatActionState state) {
        Reservation reservation = reservationRepository.findById(state.reservationId).orElse(null);
        if (reservation == null || !ownsReservation(reservation, state.guestEmail)) {
            return "I could not find that reservation for your email.";
        }
        ReservationForm form = reservationService.toReservationForm(reservation);
        form.setCheckIn(state.checkIn);
        form.setCheckOut(state.checkOut);
        try {
            Reservation updated = reservationService.updateGuestReservation(reservation.getId(), form);
            return "Reservation #" + updated.getId() + " was updated to " + updated.getCheckIn()
                    + " through " + updated.getCheckOut() + ". It remains pending hotel approval.";
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
    }

    private void absorbDetails(ChatActionState state, String question) {
        List<LocalDate> dates = extractDates(question);
        if (dates.size() >= 2) {
            state.checkIn = dates.get(0);
            state.checkOut = dates.get(1);
        } else if (dates.size() == 1 && state.action == Action.CHANGE_RESERVATION) {
            if (mentionsCheckOut(question)) {
                state.checkOut = dates.get(0);
            } else if (mentionsCheckIn(question)) {
                state.checkIn = dates.get(0);
            }
        }
        Long reservationId = extractReservationId(question);
        if (reservationId != null && state.action != Action.CREATE_RESERVATION) {
            state.reservationId = reservationId;
        }
        findRoomByName(question).ifPresent(room -> {
            state.roomId = room.getId();
            state.roomType = room.getRoomType();
        });
        RoomType roomType = extractRoomType(question);
        if (roomType != null && state.roomId == null) {
            state.roomType = roomType;
        }
        extractBudget(question).ifPresent(value -> state.maxBudget = value);
        extractCapacity(question).ifPresent(value -> state.minCapacity = value);
    }

    private Optional<Room> resolveAvailableRoom(ChatActionState state, Long excludingReservationId) {
        if (state.roomId != null) {
            return roomRepository.findById(state.roomId)
                    .filter(Room::isBookable)
                    .filter(room -> excludingReservationId == null
                            ? !reservationRepository.existsActiveOverlap(room.getId(), state.checkIn, state.checkOut)
                            : !reservationRepository.existsActiveOverlapExcludingReservation(room.getId(), excludingReservationId, state.checkIn, state.checkOut));
        }
        if (state.roomType == null) {
            return Optional.empty();
        }
        return availableRoomsByType(state.roomType, state.checkIn, state.checkOut).stream().findFirst();
    }

    private List<Room> availableRoomsByType(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        return roomRepository.findAll().stream()
                .filter(Room::isBookable)
                .filter(room -> room.getRoomType() == roomType)
                .filter(room -> !reservationRepository.existsActiveOverlap(room.getId(), checkIn, checkOut))
                .sorted(Comparator.comparing(Room::getPricePerNight))
                .toList();
    }

    private Optional<Reservation> resolvePendingReservationForGuest(ChatActionState state) {
        List<Reservation> pending = pendingReservations(state.guestEmail);
        if (state.reservationId != null) {
            return pending.stream()
                    .filter(reservation -> reservation.getId().equals(state.reservationId))
                    .findFirst();
        }
        if (state.roomId != null) {
            List<Reservation> roomMatches = pending.stream()
                    .filter(reservation -> reservation.getRoom().getId().equals(state.roomId))
                    .toList();
            if (roomMatches.size() == 1) {
                return Optional.of(roomMatches.get(0));
            }
        }
        return pending.size() == 1 ? Optional.of(pending.get(0)) : Optional.empty();
    }

    private List<Reservation> pendingReservations(String email) {
        return reservationsForGuest(email).stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                .toList();
    }

    private List<Reservation> reservationsForGuest(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return reservationRepository.findByGuestEmailOrderByCreatedAtDesc(email.trim());
    }

    private boolean ownsReservation(Reservation reservation, String email) {
        return email != null
                && reservation.getGuestEmail() != null
                && reservation.getGuestEmail().equalsIgnoreCase(email.trim());
    }

    private String formatReservations(List<Reservation> reservations) {
        StringBuilder builder = new StringBuilder();
        for (Reservation reservation : reservations) {
            builder.append("#")
                    .append(reservation.getId())
                    .append(" - ")
                    .append(reservation.getRoom().getName())
                    .append(", ")
                    .append(reservation.getCheckIn())
                    .append(" to ")
                    .append(reservation.getCheckOut())
                    .append(", ")
                    .append(reservation.getStatus())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private List<LocalDate> extractDates(String text) {
        List<DateMatch> matches = new ArrayList<>();
        addDateMatches(matches, ISO_DATE, text, this::parseIsoDate);
        addDateMatches(matches, NUMERIC_DATE, text, this::parseNumericDate);
        addDateMatches(matches, MONTH_DATE, text, this::parseMonthDate);
        Set<LocalDate> orderedDates = new LinkedHashSet<>();
        matches.stream()
                .sorted(Comparator.comparingInt(DateMatch::start))
                .map(DateMatch::date)
                .forEach(orderedDates::add);
        return orderedDates.stream().limit(2).toList();
    }

    private void addDateMatches(List<DateMatch> matches, Pattern pattern, String text, DateParser parser) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            parser.parse(matcher.group())
                    .map(date -> new DateMatch(matcher.start(), date))
                    .ifPresent(matches::add);
        }
    }

    private Optional<LocalDate> parseIsoDate(String value) {
        try {
            return Optional.of(LocalDate.parse(value));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    private Optional<LocalDate> parseMonthDate(String value) {
        String normalized = value.replace(",", "").replaceAll("\\s+", " ").trim();
        boolean hasYear = normalized.matches(".*\\b\\d{4}\\b.*");
        if (!hasYear) {
            normalized = normalized + " " + LocalDate.now().getYear();
        }
        for (DateTimeFormatter formatter : MONTH_DAY_YEAR_FORMATTERS) {
            try {
                TemporalAccessor parsed = formatter.parse(normalized);
                LocalDate date = LocalDate.of(parsed.get(ChronoField.YEAR), parsed.get(ChronoField.MONTH_OF_YEAR), parsed.get(ChronoField.DAY_OF_MONTH));
                return Optional.of(rollForwardIfPast(date, !hasYear));
            } catch (DateTimeParseException ignored) {
                // Try the next month-name format.
            }
        }
        return Optional.empty();
    }

    private Optional<LocalDate> parseNumericDate(String value) {
        String[] parts = value.replace('-', '/').split("/");
        if (parts.length < 2 || parts.length > 3) {
            return Optional.empty();
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            int year = parts.length == 3 ? normalizeYear(Integer.parseInt(parts[2])) : LocalDate.now().getYear();
            int month;
            int day;
            if (first > 12) {
                day = first;
                month = second;
            } else if (second > 12) {
                month = first;
                day = second;
            } else {
                day = first;
                month = second;
            }
            LocalDate date = LocalDate.of(year, month, day);
            return Optional.of(rollForwardIfPast(date, parts.length == 2));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private int normalizeYear(int year) {
        return year < 100 ? 2000 + year : year;
    }

    private LocalDate rollForwardIfPast(LocalDate date, boolean inferredYear) {
        return inferredYear && date.isBefore(LocalDate.now()) ? date.plusYears(1) : date;
    }

    private Long extractReservationId(String text) {
        Matcher matcher = RESERVATION_ID.matcher(text);
        if (matcher.find()) {
            return Long.valueOf(matcher.group(1));
        }
        Matcher standaloneMatcher = STANDALONE_ID.matcher(text.trim());
        if (standaloneMatcher.find()) {
            return Long.valueOf(standaloneMatcher.group(1));
        }
        return null;
    }

    private Optional<Room> findRoomByName(String text) {
        String normalized = normalize(text);
        return roomRepository.findAll().stream()
                .sorted(Comparator.comparing((Room room) -> room.getName().length()).reversed())
                .filter(room -> normalized.contains(normalize(room.getName())))
                .findFirst();
    }

    private RoomType extractRoomType(String text) {
        String normalized = normalize(text);
        if (containsAny(normalized, "single", "one person", "1 person")) {
            return RoomType.SINGLE;
        }
        if (containsAny(normalized, "double", "two people", "2 people", "queen")) {
            return RoomType.DOUBLE;
        }
        if (containsAny(normalized, "suite", "family")) {
            return RoomType.SUITE;
        }
        return null;
    }

    private Optional<BigDecimal> extractBudget(String text) {
        Matcher matcher = MONEY.matcher(text);
        if (matcher.find()) {
            return Optional.of(new BigDecimal(matcher.group(1)));
        }
        return Optional.empty();
    }

    private Optional<Integer> extractCapacity(String text) {
        Matcher matcher = CAPACITY.matcher(text);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    private String validateDateText(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            return "Check-out must be after check-in.";
        }
        if (checkIn.isBefore(LocalDate.now())) {
            return "Check-in cannot be in the past.";
        }
        return null;
    }

    private BigDecimal totalPrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPositiveConfirmation(String question) {
        String text = normalize(question);
        return containsAny(text, "yes", "yeah", "yep", "confirm", "submit it", "book it", "do it", "change it", "cancel it");
    }

    private boolean isNegativeConfirmation(String question) {
        String text = normalize(question);
        return text.equals("no") || text.equals("no thanks") || text.equals("do not") || text.equals("don't");
    }

    private boolean isAbort(String question) {
        String text = normalize(question);
        return text.equals("stop") || text.equals("never mind") || text.equals("nevermind") || text.equals("abort");
    }

    private boolean asksForRoomCount(String question) {
        String text = normalize(question);
        return containsAny(text, "how many rooms", "number of rooms", "total rooms", "rooms does the hotel have");
    }

    private boolean mentionsCheckIn(String question) {
        String text = normalize(question);
        return containsAny(text, "check-in", "check in", "arrival", "arrive");
    }

    private boolean mentionsCheckOut(String question) {
        String text = normalize(question);
        return containsAny(text, "check-out", "checkout", "check out", "departure", "leave");
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private enum Action {
        NONE,
        CREATE_RESERVATION,
        CANCEL_RESERVATION,
        CHANGE_RESERVATION,
        CHECK_AVAILABILITY,
        PRICE_QUOTE
        ,
        RECOMMEND_ROOM
    }

    private static class ChatActionState {
        private final Action action;
        private Long roomId;
        private RoomType roomType;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private Long reservationId;
        private BigDecimal maxBudget;
        private Integer minCapacity;
        private String guestName;
        private String guestEmail;
        private boolean awaitingConfirmation;

        private ChatActionState(Action action) {
            this.action = action;
        }
    }

    private record DateMatch(int start, LocalDate date) {
    }

    @FunctionalInterface
    private interface DateParser {
        Optional<LocalDate> parse(String value);
    }
}

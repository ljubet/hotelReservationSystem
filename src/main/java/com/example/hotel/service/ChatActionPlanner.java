package com.example.hotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

@Service
public class ChatActionPlanner {

    private static final Logger log = LoggerFactory.getLogger(ChatActionPlanner.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ChatActionPlanner(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public Optional<ChatActionPlan> plan(String question, String guestEmail) {
        if (question == null || question.isBlank()) {
            return Optional.of(ChatActionPlan.none());
        }
        try {
            String content = chatClient.prompt()
                    .system("""
                            You classify Aurora Hotel guest chat messages into one backend action.
                            Return only a JSON object. Do not explain.

                            Allowed actions:
                            NONE, CREATE_RESERVATION, CANCEL_RESERVATION, CHANGE_RESERVATION,
                            CHECK_AVAILABILITY, PRICE_QUOTE, RECOMMEND_ROOM, SHOW_RESERVATIONS, ROOM_COUNT.

                            Fill fields only when the guest provided them:
                            action, roomName, roomType, checkIn, checkOut, reservationId, maxBudget, minCapacity.

                            roomType must be SINGLE, DOUBLE, SUITE, or null.
                            Dates should be normalized to yyyy-MM-dd when obvious. If the guest used a natural
                            date you cannot normalize confidently, copy it exactly into checkIn/checkOut.
                            If the guest asks to see, list, show, or check their existing bookings, use SHOW_RESERVATIONS.
                            For SHOW_RESERVATIONS with dates that do not include a year, copy the natural date text
                            instead of guessing a future year.
                            Do not use CHECK_AVAILABILITY for existing reservation/status questions.
                            If this is just a normal hotel question, use action NONE.
                            """)
                    .user("""
                            Guest email: %s
                            Guest message: %s

                            JSON:
                            """.formatted(blankToUnknown(guestEmail), question))
                    .call()
                    .content();
            return Optional.of(parse(content));
        } catch (RuntimeException ex) {
            log.warn("Could not create AI chat action plan.", ex);
            return Optional.empty();
        }
    }

    private ChatActionPlan parse(String content) {
        if (content == null || content.isBlank()) {
            return ChatActionPlan.none();
        }
        String json = extractJson(content);
        try {
            JsonNode root = objectMapper.readTree(json);
            return new ChatActionPlan(
                    text(root, "action", "NONE").toUpperCase(Locale.ROOT),
                    text(root, "roomName", null),
                    normalizeRoomType(text(root, "roomType", null)),
                    text(root, "checkIn", null),
                    text(root, "checkOut", null),
                    longValue(root, "reservationId"),
                    decimalValue(root, "maxBudget"),
                    intValue(root, "minCapacity")
            );
        } catch (Exception ex) {
            log.warn("AI chat action plan was not valid JSON: {}", content);
            return ChatActionPlan.none();
        }
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private String text(JsonNode root, String field, String fallback) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return fallback;
        }
        String value = node.asText();
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim()) ? fallback : value.trim();
    }

    private String normalizeRoomType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SINGLE", "DOUBLE", "SUITE" -> normalized;
            default -> null;
        };
    }

    private Long longValue(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.canConvertToLong()) {
            return node.asLong();
        }
        try {
            return Long.parseLong(node.asText().trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Integer intValue(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.canConvertToInt()) {
            return node.asInt();
        }
        try {
            return Integer.parseInt(node.asText().trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private BigDecimal decimalValue(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        try {
            return new BigDecimal(node.asText().trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}

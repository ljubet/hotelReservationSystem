package com.example.hotel.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Optional;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    public static final String UNAVAILABLE_REPLY =
            "Our virtual assistant is currently unavailable. Your message has been saved and our team will reply shortly.";

    private final ChatClient chatClient;
    private final HotelMcpTools hotelMcpTools;
    private final ChatActionPlanner chatActionPlanner;
    private final ChatActionService chatActionService;
    private final String systemPrompt;
    private final AtomicBoolean toolCallingDisabled = new AtomicBoolean(false);

    public ChatbotService(ChatClient.Builder chatClientBuilder,
                          HotelMcpTools hotelMcpTools,
                          ChatActionPlanner chatActionPlanner,
                          ChatActionService chatActionService,
                          @Value("${hotel.chatbot.system-prompt}") String systemPrompt) {
        this.chatClient = chatClientBuilder.build();
        this.hotelMcpTools = hotelMcpTools;
        this.chatActionPlanner = chatActionPlanner;
        this.chatActionService = chatActionService;
        this.systemPrompt = systemPrompt;
    }

    public String getReply(String question, String guestEmail, String guestName) {
        String email = blankToUnknown(guestEmail);
        Optional<ChatActionPlan> plannedAction = chatActionPlanner.plan(question, guestEmail);
        String actionReply = plannedAction.isPresent()
                ? chatActionService.handlePlannedAction(question, guestName, guestEmail, plannedAction.get()).orElse(null)
                : chatActionService.handle(question, guestName, guestEmail).orElse(null);
        if (actionReply != null) {
            return actionReply;
        }
        if (toolCallingDisabled.get()) {
            return getReplyWithInlineContext(question, email);
        }
        try {
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user("""
                            Guest email: %s
                            Guest question: %s
                            """.formatted(email, question))
                    .tools(hotelMcpTools)
                    .call()
                    .content();
            return normalize(content);
        } catch (RuntimeException ex) {
            if (isToolUnsupported(ex)) {
                toolCallingDisabled.set(true);
                log.warn("The configured Ollama model does not support tool calls. Using inline hotel context for this app run.");
            } else {
                log.warn("Ollama tool call failed. Retrying with inline hotel context.", ex);
            }
            return getReplyWithInlineContext(question, email);
        }
    }

    private String getReplyWithInlineContext(String question, String guestEmail) {
        try {
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user("""
                            Use this live hotel context to answer the guest. Do not mention internal tools.

                            %s

                            %s

                            %s

                            %s

                            Guest email: %s
                            Guest question: %s
                            """.formatted(
                            hotelMcpTools.getHotelInfo(),
                            hotelMcpTools.getRoomCount(),
                            hotelMcpTools.getRoomList(),
                            reservationContext(guestEmail),
                            guestEmail,
                            question))
                    .call()
                    .content();
            return normalize(content);
        } catch (RuntimeException ex) {
            log.warn("Ollama inline-context chat failed.", ex);
            return UNAVAILABLE_REPLY;
        }
    }

    private String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String reservationContext(String guestEmail) {
        if ("unknown".equals(guestEmail)) {
            return "No guest email was provided for reservation lookup.";
        }
        return hotelMcpTools.getReservationStatus(guestEmail);
    }

    private String normalize(String content) {
        if (content == null || content.isBlank()) {
            return UNAVAILABLE_REPLY;
        }
        return content.trim();
    }

    private boolean isToolUnsupported(RuntimeException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("does not support tools");
    }
}

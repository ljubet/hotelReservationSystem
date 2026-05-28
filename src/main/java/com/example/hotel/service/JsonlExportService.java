package com.example.hotel.service;

import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationTurn;
import com.example.hotel.entity.MessageRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JsonlExportService {

    private final ObjectMapper objectMapper;

    public JsonlExportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJsonl(List<Conversation> conversations) {
        StringBuilder builder = new StringBuilder();
        for (Conversation conversation : conversations) {
            try {
                builder.append(objectMapper.writeValueAsString(toFineTuningObject(conversation))).append('\n');
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Could not serialize conversation " + conversation.getId(), ex);
            }
        }
        return builder.toString();
    }

    private Map<String, Object> toFineTuningObject(Conversation conversation) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are a helpful hotel assistant."));
        for (ConversationTurn turn : conversation.getTurns()) {
            messages.add(Map.of(
                    "role", turn.getRole() == MessageRole.USER ? "user" : "assistant",
                    "content", turn.getContent()));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messages", messages);
        return payload;
    }
}

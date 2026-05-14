package com.example.hotel.service;

import com.example.hotel.dto.ConversationCreateRequest;
import com.example.hotel.dto.ConversationDetailsResponse;
import com.example.hotel.dto.ConversationMessageCreateRequest;
import com.example.hotel.dto.ConversationMessageResponse;
import com.example.hotel.dto.ConversationMessageUpdateRequest;
import com.example.hotel.dto.ConversationResponse;
import com.example.hotel.dto.ConversationUpdateRequest;
import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.ConversationMessage;
import com.example.hotel.entity.Hotel;
import com.example.hotel.entity.MessageRole;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.repository.ConversationMessageRepository;
import com.example.hotel.repository.ConversationRepository;
import com.example.hotel.repository.HotelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final HotelRepository hotelRepository;
    private final ObjectMapper objectMapper;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMessageRepository conversationMessageRepository,
                               HotelRepository hotelRepository,
                               ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.hotelRepository = hotelRepository;
        this.objectMapper = objectMapper;
    }

    public List<ConversationResponse> getAll() {
        return conversationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ConversationDetailsResponse getById(Long id) {
        Conversation conversation = findConversation(id);
        List<ConversationMessageResponse> messages = conversationMessageRepository
                .findByConversationIdOrderByOrderNumberAsc(id)
                .stream()
                .map(this::toMessageResponse)
                .toList();
        long messageCount = conversationMessageRepository.countByConversationId(id);
        return toDetailsResponse(conversation, messageCount, messages);
    }

    @Transactional
    public ConversationResponse create(ConversationCreateRequest request) {
        Conversation conversation = new Conversation();
        apply(request, conversation);
        return toResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public ConversationResponse update(Long id, ConversationUpdateRequest request) {
        Conversation conversation = findConversation(id);
        apply(request, conversation);
        return toResponse(conversationRepository.save(conversation));
    }

    public void delete(Long id) {
        Conversation conversation = findConversation(id);
        conversationRepository.delete(conversation);
    }

    @Transactional
    public ConversationMessageResponse addMessage(Long conversationId, ConversationMessageCreateRequest request) {
        Conversation conversation = findConversation(conversationId);
        validateOrderNumber(request.orderNumber());
        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setRole(request.role());
        message.setContent(request.content());
        message.setOrderNumber(request.orderNumber());
        return toMessageResponse(conversationMessageRepository.save(message));
    }

    @Transactional
    public ConversationMessageResponse updateMessage(Long conversationId, Long messageId,
                                                     ConversationMessageUpdateRequest request) {
        Conversation conversation = findConversation(conversationId);
        ConversationMessage message = findMessage(messageId);
        validateConversationMatch(conversation, message);
        validateOrderNumber(request.orderNumber());
        message.setRole(request.role());
        message.setContent(request.content());
        message.setOrderNumber(request.orderNumber());
        return toMessageResponse(conversationMessageRepository.save(message));
    }

    @Transactional
    public void deleteMessage(Long conversationId, Long messageId) {
        Conversation conversation = findConversation(conversationId);
        ConversationMessage message = findMessage(messageId);
        validateConversationMatch(conversation, message);
        conversationMessageRepository.delete(message);
    }

    public String exportConversationAsJson(Long conversationId) {
        Conversation conversation = findConversation(conversationId);
        List<ConversationMessage> messages = conversationMessageRepository
                .findByConversationIdOrderByOrderNumberAsc(conversationId);
        List<Map<String, Object>> exportMessages = toExportMessages(messages);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messages", exportMessages);
        return writeJson(payload, "conversation", conversation.getId());
    }

    public String exportAllConversationsAsJson() {
        List<Conversation> conversations = conversationRepository.findAll();
        List<Map<String, Object>> export = new ArrayList<>();
        for (Conversation conversation : conversations) {
            List<ConversationMessage> messages = conversationMessageRepository
                    .findByConversationIdOrderByOrderNumberAsc(conversation.getId());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messages", toExportMessages(messages));
            export.add(payload);
        }
        return writeJson(export, "conversations", null);
    }

    public List<ConversationResponse> filterByCategory(ConversationCategory category) {
        return conversationRepository.findByCategory(category).stream().map(this::toResponse).toList();
    }

    public List<ConversationResponse> filterByHotel(Long hotelId) {
        return conversationRepository.findByHotelId(hotelId).stream().map(this::toResponse).toList();
    }

    public List<ConversationResponse> filterByLanguage(String language) {
        return conversationRepository.findByLanguageIgnoreCase(language).stream().map(this::toResponse).toList();
    }

    private Conversation findConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + id));
    }

    private ConversationMessage findMessage(Long id) {
        return conversationMessageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Conversation message not found: " + id));
    }

    private void validateOrderNumber(Integer orderNumber) {
        if (orderNumber == null || orderNumber <= 0) {
            throw new BadRequestException("orderNumber must be a positive number");
        }
    }

    private void validateConversationMatch(Conversation conversation, ConversationMessage message) {
        if (!message.getConversation().getId().equals(conversation.getId())) {
            throw new BadRequestException("Message does not belong to conversation: " + conversation.getId());
        }
    }

    private void apply(ConversationCreateRequest request, Conversation conversation) {
        conversation.setTitle(request.title());
        conversation.setCategory(request.category());
        conversation.setLanguage(resolveLanguage(request.language()));
        conversation.setDescription(request.description());
        conversation.setHotel(resolveHotel(request.hotelId()));
    }

    private void apply(ConversationUpdateRequest request, Conversation conversation) {
        conversation.setTitle(request.title());
        conversation.setCategory(request.category());
        conversation.setLanguage(resolveLanguage(request.language()));
        conversation.setDescription(request.description());
        conversation.setHotel(resolveHotel(request.hotelId()));
    }

    private String resolveLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "English";
        }
        return language;
    }

    private Hotel resolveHotel(Long hotelId) {
        if (hotelId == null) {
            return null;
        }
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new NotFoundException("Hotel not found: " + hotelId));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        long messageCount = conversationMessageRepository.countByConversationId(conversation.getId());
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCategory(),
                conversation.getLanguage(),
                conversation.getDescription(),
                conversation.getHotel() == null ? null : conversation.getHotel().getId(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messageCount
        );
    }

    private ConversationDetailsResponse toDetailsResponse(Conversation conversation, long messageCount,
                                                         List<ConversationMessageResponse> messages) {
        return new ConversationDetailsResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCategory(),
                conversation.getLanguage(),
                conversation.getDescription(),
                conversation.getHotel() == null ? null : conversation.getHotel().getId(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messageCount,
                messages
        );
    }

    private ConversationMessageResponse toMessageResponse(ConversationMessage message) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getRole(),
                message.getContent(),
                message.getOrderNumber(),
                message.getCreatedAt()
        );
    }

    private List<Map<String, Object>> toExportMessages(List<ConversationMessage> messages) {
        List<Map<String, Object>> export = new ArrayList<>();
        for (ConversationMessage message : messages) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("role", message.getRole().name().toLowerCase(Locale.ROOT));
            entry.put("content", message.getContent());
            export.add(entry);
        }
        return export;
    }

    private String writeJson(Object payload, String label, Long id) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            String suffix = id == null ? "" : ": " + id;
            throw new IllegalStateException("Failed to export " + label + suffix, ex);
        }
    }

    @Transactional
    public int generateConversations(int count) {
        if (count <= 0) {
            throw new BadRequestException("count must be a positive number");
        }
        List<Hotel> hotels = hotelRepository.findAll();
        ConversationCategory[] categories = ConversationCategory.values();
        Random random = new Random();
        List<Conversation> conversations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Conversation conversation = new Conversation();
            ConversationCategory category = categories[random.nextInt(categories.length)];
            conversation.setTitle("Generated conversation " + (i + 1));
            conversation.setCategory(category);
            conversation.setLanguage("English");
            conversation.setDescription("Auto-generated sample conversation.");
            if (!hotels.isEmpty()) {
                conversation.setHotel(hotels.get(i % hotels.size()));
            }
            List<ConversationMessage> messages = new ArrayList<>();
            messages.add(buildMessage(conversation, MessageRole.SYSTEM, 1,
                    "You are a helpful hotel assistant."));
            messages.add(buildMessage(conversation, MessageRole.USER, 2, sampleUserMessage(category)));
            messages.add(buildMessage(conversation, MessageRole.ASSISTANT, 3, sampleAssistantMessage(category)));
            conversation.setMessages(messages);
            conversations.add(conversation);
        }
        conversationRepository.saveAll(conversations);
        return conversations.size();
    }

    private ConversationMessage buildMessage(Conversation conversation, MessageRole role, int orderNumber, String content) {
        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setRole(role);
        message.setOrderNumber(orderNumber);
        message.setContent(content);
        return message;
    }

    private String sampleUserMessage(ConversationCategory category) {
        return switch (category) {
            case PARKING -> "Do you have parking?";
            case BREAKFAST -> "Is breakfast included?";
            case CHECK_IN -> "What time is check-in?";
            case CHECK_OUT -> "What time is check-out?";
            case CANCELLATION -> "What is your cancellation policy?";
            case PAYMENT -> "What payment methods do you accept?";
            case ROOM_SERVICE -> "Do you offer room service?";
            case COMPLAINT -> "My room was not cleaned today.";
            case RESERVATION -> "Can I book a double room for two nights?";
            case GENERAL_INFORMATION -> "Can you tell me about the hotel amenities?";
        };
    }

    private String sampleAssistantMessage(ConversationCategory category) {
        return switch (category) {
            case PARKING -> "Yes, we offer parking for all hotel guests.";
            case BREAKFAST -> "Breakfast is served daily and can be included with your stay.";
            case CHECK_IN -> "Check-in starts at 3 PM, and early check-in depends on availability.";
            case CHECK_OUT -> "Check-out is at 11 AM; late check-out may be available upon request.";
            case CANCELLATION -> "Cancellations are free up to 24 hours before arrival for flexible rates.";
            case PAYMENT -> "We accept major credit cards and contactless payments.";
            case ROOM_SERVICE -> "Room service is available during posted hours. Let us know your preferences.";
            case COMPLAINT -> "I'm sorry to hear that. We'll have housekeeping address it right away.";
            case RESERVATION -> "Yes, we have double rooms available. Please share your dates.";
            case GENERAL_INFORMATION -> "We offer Wi-Fi, a fitness center, and a concierge to assist you.";
        };
    }
}

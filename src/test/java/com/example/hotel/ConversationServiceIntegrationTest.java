package com.example.hotel;

import com.example.hotel.dto.ChatSimulationRequest;
import com.example.hotel.dto.ConversationCreateRequest;
import com.example.hotel.dto.ConversationMessageCreateRequest;
import com.example.hotel.dto.ConversationMessageUpdateRequest;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.Hotel;
import com.example.hotel.entity.MessageRole;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.service.ChatbotSimulationService;
import com.example.hotel.service.ConversationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ConversationServiceIntegrationTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private ChatbotSimulationService chatbotSimulationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsConversation() {
        Hotel hotel = buildHotel();
        hotelRepository.save(hotel);

        ConversationCreateRequest request = new ConversationCreateRequest(
                "Parking question",
                ConversationCategory.PARKING,
                "English",
                "Guest asks about parking",
                hotel.getId()
        );

        var response = conversationService.create(request);
        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo("Parking question");
        assertThat(response.category()).isEqualTo(ConversationCategory.PARKING);
        assertThat(response.language()).isEqualTo("English");
        assertThat(response.hotelId()).isEqualTo(hotel.getId());
        assertThat(response.messageCount()).isZero();
    }

    @Test
    void addsMessageToConversation() {
        Long conversationId = createConversation();

        ConversationMessageCreateRequest request = new ConversationMessageCreateRequest(
                MessageRole.USER,
                "Do you have parking?",
                2
        );

        var message = conversationService.addMessage(conversationId, request);
        assertThat(message.id()).isNotNull();
        assertThat(message.conversationId()).isEqualTo(conversationId);
        assertThat(message.role()).isEqualTo(MessageRole.USER);
        assertThat(message.orderNumber()).isEqualTo(2);
    }

    @Test
    void exportsConversationJsonWithMessages() throws Exception {
        Long conversationId = createConversation();
        conversationService.addMessage(conversationId, new ConversationMessageCreateRequest(
                MessageRole.SYSTEM,
                "You are a helpful hotel assistant.",
                1
        ));
        conversationService.addMessage(conversationId, new ConversationMessageCreateRequest(
                MessageRole.USER,
                "Do you have parking?",
                2
        ));
        conversationService.addMessage(conversationId, new ConversationMessageCreateRequest(
                MessageRole.ASSISTANT,
                "Yes, we offer free parking for all guests.",
                3
        ));

        String json = conversationService.exportConversationAsJson(conversationId);
        JsonNode root = objectMapper.readTree(json);
        assertThat(root.has("messages")).isTrue();
        assertThat(root.get("messages").isArray()).isTrue();
        assertThat(root.get("messages")).hasSize(3);
    }

    @Test
    void simulationReturnsParkingResponse() {
        var response = chatbotSimulationService.simulate(new ChatSimulationRequest("Is parking available?"));
        assertThat(response.matchedCategory()).isEqualTo(ConversationCategory.PARKING);
        assertThat(response.assistantResponse()).contains("Simulation");
    }

    @Test
    void invalidMessageConversationRelationThrows() {
        Long conversationId = createConversation();
        Long otherConversationId = createConversation();

        var message = conversationService.addMessage(conversationId, new ConversationMessageCreateRequest(
                MessageRole.USER,
                "Is breakfast included?",
                1
        ));

        ConversationMessageUpdateRequest request = new ConversationMessageUpdateRequest(
                MessageRole.USER,
                "Update",
                1
        );

        assertThatThrownBy(() -> conversationService.updateMessage(otherConversationId, message.id(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Message does not belong to conversation");
    }

    private Long createConversation() {
        Hotel hotel = buildHotel();
        hotelRepository.save(hotel);
        ConversationCreateRequest request = new ConversationCreateRequest(
                "Sample",
                ConversationCategory.GENERAL_INFORMATION,
                "English",
                "Sample conversation",
                hotel.getId()
        );
        return conversationService.create(request).id();
    }

    private Hotel buildHotel() {
        Hotel hotel = new Hotel();
        hotel.setName("City Hotel");
        hotel.setDescription("Central location");
        hotel.setAddress("99 Main St");
        hotel.setCity("Metro");
        hotel.setCountry("USA");
        hotel.setPhone("+1-555-0111");
        hotel.setEmail("hello@city.example");
        hotel.setStarRating(3);
        return hotel;
    }
}


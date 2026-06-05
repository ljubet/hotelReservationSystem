package com.example.hotel.service;

import com.example.hotel.entity.ChatMessage;
import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.ConversationStatus;
import com.example.hotel.entity.ConversationTurn;
import com.example.hotel.entity.MessageRole;
import com.example.hotel.form.ConversationForm;
import com.example.hotel.repository.ConversationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public Conversation saveFromForm(ConversationForm form, Long id) {
        validateTurns(form.getTurns());
        Conversation conversation = id == null ? new Conversation() : conversationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        conversation.setTitle(form.getTitle());
        conversation.setCategory(form.getCategory());
        List<ConversationTurn> turns = form.getTurns().stream()
                .map(turnForm -> {
                    ConversationTurn turn = new ConversationTurn();
                    turn.setRole(turnForm.getRole());
                    turn.setContent(turnForm.getContent());
                    return turn;
                })
                .toList();
        conversation.replaceTurns(turns);
        return conversationRepository.save(conversation);
    }

    @Transactional
    public Conversation createFromChat(ChatMessage message) {
        return createFromChat(message, message.isAiAnswered());
    }

    @Transactional
    public Conversation createFromChat(ChatMessage message, boolean aiCorrected) {
        Conversation conversation = new Conversation();
        conversation.setTitle("Chat with " + message.getSenderName());
        conversation.setCategory(ConversationCategory.GENERAL);
        conversation.setStatus(ConversationStatus.DRAFT);
        conversation.setAiCorrected(aiCorrected);

        ConversationTurn userTurn = new ConversationTurn();
        userTurn.setRole(MessageRole.USER);
        userTurn.setContent(message.getQuestion());
        ConversationTurn assistantTurn = new ConversationTurn();
        assistantTurn.setRole(MessageRole.ASSISTANT);
        assistantTurn.setContent(message.getAnswer());
        conversation.replaceTurns(List.of(userTurn, assistantTurn));
        return conversationRepository.save(conversation);
    }

    public ConversationForm toForm(Conversation conversation) {
        ConversationForm form = new ConversationForm();
        form.setTitle(conversation.getTitle());
        form.setCategory(conversation.getCategory());
        List<ConversationForm.TurnForm> turns = new ArrayList<>();
        for (ConversationTurn turn : conversation.getTurns()) {
            ConversationForm.TurnForm turnForm = new ConversationForm.TurnForm();
            turnForm.setRole(turn.getRole());
            turnForm.setContent(turn.getContent());
            turns.add(turnForm);
        }
        form.setTurns(turns);
        return form;
    }

    public void validateTurns(List<ConversationForm.TurnForm> turns) {
        if (turns == null || turns.isEmpty()) {
            throw new IllegalArgumentException("Conversation must include at least one turn.");
        }
        MessageRole expected = MessageRole.USER;
        for (ConversationForm.TurnForm turn : turns) {
            if (turn.getRole() != expected) {
                throw new IllegalArgumentException("Turns must start with USER and alternate USER/ASSISTANT.");
            }
            if (!StringUtils.hasText(turn.getContent())) {
                throw new IllegalArgumentException("Turn content cannot be blank.");
            }
            expected = expected == MessageRole.USER ? MessageRole.ASSISTANT : MessageRole.USER;
        }
    }
}

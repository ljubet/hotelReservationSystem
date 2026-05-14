package com.example.hotel.repository;

import com.example.hotel.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByConversationIdOrderByOrderNumberAsc(Long conversationId);

    long countByConversationId(Long conversationId);
}


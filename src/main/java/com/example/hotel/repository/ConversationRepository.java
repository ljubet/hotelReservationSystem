package com.example.hotel.repository;

import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByCategoryOrderByCreatedAtDesc(ConversationCategory category);

    List<Conversation> findByStatusOrderByCreatedAtDesc(ConversationStatus status);

    List<Conversation> findByCategoryAndStatusOrderByCreatedAtDesc(ConversationCategory category, ConversationStatus status);

    List<Conversation> findAllByOrderByCreatedAtDesc();
}


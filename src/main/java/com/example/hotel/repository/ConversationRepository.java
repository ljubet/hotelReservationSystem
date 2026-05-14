package com.example.hotel.repository;

import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByCategory(ConversationCategory category);

    List<Conversation> findByHotelId(Long hotelId);

    List<Conversation> findByLanguageIgnoreCase(String language);
}


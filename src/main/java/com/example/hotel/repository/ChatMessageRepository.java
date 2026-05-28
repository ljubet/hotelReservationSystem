package com.example.hotel.repository;

import com.example.hotel.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    long countByAnsweredFalse();

    List<ChatMessage> findTop5ByAnsweredFalseOrderByCreatedAtDesc();

    List<ChatMessage> findByAnsweredFalseOrderByCreatedAtDesc();

    List<ChatMessage> findAllByOrderByCreatedAtDesc();

    List<ChatMessage> findBySenderEmailOrderByCreatedAtAsc(String senderEmail);
}

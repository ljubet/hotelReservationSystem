package com.example.hotel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String senderName;

    @Email
    @NotBlank
    @Column(nullable = false)
    private String senderEmail;

    @NotBlank
    @Column(nullable = false, length = 5000)
    private String question;

    @Column(length = 5000)
    private String answer;

    private LocalDateTime answeredAt;

    @Column(nullable = false)
    private boolean answered = false;

    @Column(nullable = false)
    private boolean savedAsConversation = false;

    private Boolean aiAnswered = false;

    private Boolean aiCorrect;

    private Boolean escalatedToAdmin = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        normalizeFlags();
    }

    @PostLoad
    @PreUpdate
    void normalizeFlags() {
        if (this.aiAnswered == null) {
            this.aiAnswered = false;
        }
        if (this.escalatedToAdmin == null) {
            this.escalatedToAdmin = false;
        }
    }

    public boolean isAiAnswered() {
        return Boolean.TRUE.equals(aiAnswered);
    }

    public boolean isEscalatedToAdmin() {
        return Boolean.TRUE.equals(escalatedToAdmin);
    }
}

package com.example.hotel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_user_id")
    private User guestUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull
    @Column(nullable = false)
    private LocalDate checkIn;

    @NotNull
    @Column(nullable = false)
    private LocalDate checkOut;

    @NotBlank
    @Column(nullable = false)
    private String guestName;

    @Email
    @NotBlank
    @Column(nullable = false)
    private String guestEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column
    private PaymentOption paymentOption = PaymentOption.PAY_AT_HOTEL;

    @Column
    private Boolean simulatedPaid = false;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(length = 2000)
    private String staffNote;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        normalizePayment();
    }

    @PostLoad
    @PreUpdate
    void normalizePayment() {
        if (paymentOption == null) {
            paymentOption = PaymentOption.PAY_AT_HOTEL;
        }
        if (simulatedPaid == null) {
            simulatedPaid = false;
        }
    }

    public boolean isSimulatedPaid() {
        return Boolean.TRUE.equals(simulatedPaid);
    }
}


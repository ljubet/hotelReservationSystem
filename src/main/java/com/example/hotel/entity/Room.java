package com.example.hotel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 2000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType;

    @NotNull
    @DecimalMin("1.00")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    @Column(precision = 2, scale = 1)
    private BigDecimal rating = new BigDecimal("4.7");

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean underRenovation = false;

    @Enumerated(EnumType.STRING)
    @Column
    private HousekeepingStatus housekeepingStatus = HousekeepingStatus.CLEAN;

    @Column(length = 1000)
    private String amenities;

    public boolean isBookable() {
        return available && !underRenovation && housekeepingStatus != HousekeepingStatus.OUT_OF_SERVICE;
    }

    public void setUnderRenovation(boolean underRenovation) {
        this.underRenovation = underRenovation;
        if (underRenovation) {
            this.available = false;
        }
    }

    @PrePersist
    @PreUpdate
    void normalizeAvailability() {
        if (underRenovation) {
            available = false;
        }
        if (rating == null) {
            rating = new BigDecimal("4.7");
        }
        if (housekeepingStatus == null) {
            housekeepingStatus = HousekeepingStatus.CLEAN;
        }
        if (housekeepingStatus == HousekeepingStatus.OUT_OF_SERVICE) {
            available = false;
        }
    }

    @PostLoad
    void normalizeHousekeepingStatus() {
        if (housekeepingStatus == null) {
            housekeepingStatus = HousekeepingStatus.CLEAN;
        }
    }
}


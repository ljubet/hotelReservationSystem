package com.example.hotel.form;

import com.example.hotel.entity.HousekeepingStatus;
import com.example.hotel.entity.RoomType;
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
public class RoomForm {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    private RoomType roomType;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal pricePerNight;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal rating = new BigDecimal("4.7");

    @NotNull
    @Min(1)
    private Integer capacity;

    private String imageUrl;

    private boolean available = true;

    private boolean underRenovation;

    @NotNull
    private HousekeepingStatus housekeepingStatus = HousekeepingStatus.CLEAN;

    private String amenities;
}

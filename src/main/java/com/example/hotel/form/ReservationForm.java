package com.example.hotel.form;

import com.example.hotel.entity.PaymentOption;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReservationForm {

    @NotNull
    private Long roomId;

    @NotNull
    @FutureOrPresent
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    @NotBlank
    private String guestName;

    @Email
    @NotBlank
    private String guestEmail;

    @NotNull
    private PaymentOption paymentOption = PaymentOption.PAY_AT_HOTEL;
}

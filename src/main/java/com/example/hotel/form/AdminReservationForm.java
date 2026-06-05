package com.example.hotel.form;

import com.example.hotel.entity.PaymentOption;
import com.example.hotel.entity.ReservationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdminReservationForm {

    @NotNull
    private Long roomId;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    @NotBlank
    private String guestName;

    @Email
    @NotBlank
    private String guestEmail;

    @NotNull
    private ReservationStatus status;

    @NotNull
    private PaymentOption paymentOption = PaymentOption.PAY_AT_HOTEL;

    private String staffNote;
}

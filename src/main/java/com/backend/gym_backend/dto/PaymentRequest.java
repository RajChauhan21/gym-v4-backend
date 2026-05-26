package com.backend.gym_backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PaymentRequest {

    private Integer ownerId;

    private Integer paymentId;

    private Integer amountPaid;

    private LocalDate date;

    private String method;

    private Integer memberId;
}

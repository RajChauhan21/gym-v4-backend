package com.backend.gym_backend.dto;

import com.backend.gym_backend.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PaymentResponse {

    private Integer paymentId;

    private Integer amountPaid;

    private LocalDate date;

    private String method;

    private Member member;

}

package com.backend.gym_backend.dto;

import java.time.LocalDate;


public interface PaymentProjection {

    Long getPaymentId();

    Long getMemberId();

    String getMemberName();

    Double getAmount();

    Double getDueAmount();

    String getMethod();

    LocalDate getPaymentDate();

    String getMembershipName();
}

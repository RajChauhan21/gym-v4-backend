package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OwnerPaymentHistoryResponse {

    private LocalDateTime paymentDateTime;

    private String invoiceLink;

    private BigDecimal amount;

    private String status;
}

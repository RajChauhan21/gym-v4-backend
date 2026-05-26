package com.backend.gym_backend.dto;

import java.math.BigDecimal;

public interface OwnerPaymentProjection {
    BigDecimal getAmount();
    String getStatus();
    String getMethod();
    java.time.LocalDateTime getCreatedAt();
    String getInvoiceUrl();
}

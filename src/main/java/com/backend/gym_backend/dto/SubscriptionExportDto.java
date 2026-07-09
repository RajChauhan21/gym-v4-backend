package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionExportDto {
    private BigDecimal amount;
    private String method;
    private String status;
    private LocalDateTime dateTime;
    private String receipt;
}

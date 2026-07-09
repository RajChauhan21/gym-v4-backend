package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PaymentExportDto {

    private Long paymentId;
    private String name;
    private String plan;
    private Double paidAmount;
    private Double dueAmount;
    private String method;
    private LocalDate date;
}

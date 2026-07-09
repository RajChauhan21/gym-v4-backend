package com.backend.gym_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDTO {

    private Integer subtotal;

    private BigDecimal discount;

    private BigDecimal gst;

    private Integer total;

    private Integer paidAmount;

    private Integer dueAmount;

    private String paymentMethod;

    private String transactionId;
}

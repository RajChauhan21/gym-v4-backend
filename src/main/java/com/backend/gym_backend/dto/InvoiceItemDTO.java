package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InvoiceItemDTO {

    private String description;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal total;
}

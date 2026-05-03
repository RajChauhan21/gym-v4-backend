package com.backend.gym_backend.dto;

import lombok.Data;

@Data
public class InvoiceCreatedEvent {

    private final Integer invoiceId;
    private final String paymentId;
    private final String subscriptionId;
}

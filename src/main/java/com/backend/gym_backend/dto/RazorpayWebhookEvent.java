package com.backend.gym_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayWebhookEvent {
    private String event;
    private Payload payload;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        // All possible entities in any Razorpay webhook
        private SubscriptionWrapper subscription;
        private PaymentWrapper payment;
        private InvoiceWrapper invoice;
    }

    @Data
    public static class SubscriptionWrapper {
        private SubscriptionEntity entity;
    }

    @Data
    public static class PaymentWrapper {
        private PaymentEntity entity;
    }

    @Data
    public static class InvoiceWrapper {
        private InvoiceEntity entity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscriptionEntity {
        private String id;
        private String customer_email;
        private String customer_contact;
        private Long start_at;
        private Long end_at;
        private Long current_end;  //when the payment attempt was made
        private String payment_method;
        // ... other fields
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentEntity {
        private String id;
        private int amount;
        private String method;
        private String status;
        private String invoice_id;
        private String email;
        private String contact;
        private String upi_transaction_id;
    }

    // Add InvoiceEntity similarly
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InvoiceEntity {
        private String id;
        private Long amount;
        private String status;
        private String method;
        private String short_url;
        private String paid_at;
    }
}


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
        private String status;
        private Long end_at;
        private Long charge_at;
        private Long current_end;
        private Long current_start;  //when the payment attempt was made
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
        private String currency;
        private String created_at;
    }

    // Add InvoiceEntity similarly
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InvoiceEntity {
        private String id;
        private Integer amount;
        private Integer amount_paid;
        private String status;
        private String method;
        private String currency;
        private String short_url;
        private Long paid_at;      // Changed to Long for epoch
        private Long issued_at;      // Changed to Long for epoch
        private Long created_at;   // Changed to Long for epoch
        private String subscription_id;
        private String payment_id;
        private String billing_start;
        private String billing_end;

        // Nested object for customer details
        private CustomerDetails customer_details;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CustomerDetails {
            private String id;
            private String name;
            private String email;
            private String contact;
            private String gstin;
            private String customer_name;
            private String customer_email;
            private String customer_contact;
            // Billing/Shipping address would be Maps or specific DTOs if needed
        }
    }

}


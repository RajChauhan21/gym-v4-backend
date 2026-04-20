package com.backend.gym_backend.response;

import lombok.Data;

@Data
public class PaymentCaptureEvent {

    private Payload payload;

    @Data
    public static class Payload {
        private Payment payment;
    }

    @Data
    public static class Payment {
        private Entity entity;
    }

    @Data
    public static class Entity {
        private String id;
        private int amount;
        private String method;
        private String status;
        private String invoice_id;
        private String email;
        private String contact;
        private String upi_transaction_id;
    }
}

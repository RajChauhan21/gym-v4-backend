package com.backend.gym_backend.response;

import lombok.Data;

@Data
public class PaymentCaptureEvent {

    private Payload payload;

    public static class Payload {
        private Payment payment;
    }

    public static class Payment {
        private Entity entity;
    }

    public static class Entity {
        private String id;
        private int amount;
    }
}

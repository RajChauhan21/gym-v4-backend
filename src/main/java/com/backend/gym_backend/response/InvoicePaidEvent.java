package com.backend.gym_backend.response;

import lombok.Data;

@Data
public class InvoicePaidEvent {

    private Payload payload;

    public static class Payload {
        public Invoice invoice;
    }

    public static class Invoice {
        public Entity entity;
    }

    public static class Entity {
        private String id;
        private String subscription_id;
        private int amount_paid;
    }
}

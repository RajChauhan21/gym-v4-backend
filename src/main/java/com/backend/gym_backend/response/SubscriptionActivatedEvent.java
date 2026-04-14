package com.backend.gym_backend.response;

import lombok.Data;

@Data
public class SubscriptionActivatedEvent {

    private Payload payload;

    static class Payload {
        private Subscription subscription;
    }

    static class Subscription {
        private Entity entity;
    }

    static class Entity {
        private String id;
    }
}

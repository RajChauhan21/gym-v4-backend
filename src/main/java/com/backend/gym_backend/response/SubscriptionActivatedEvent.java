package com.backend.gym_backend.response;

import lombok.Data;

@Data
public class SubscriptionActivatedEvent {

    private Payload payload;

    @Data
   public static class Payload {
        private Subscription subscription;
    }

    @Data
   public static class Subscription {
        private Entity entity;
    }

    @Data
   public static class Entity {
        private String id;
    }
}

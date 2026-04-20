package com.backend.gym_backend.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
        private String customer_email;
        private String customer_contact;
        private Long start_at;
        private Long end_at;
        private Long current_end;  //when the payment attempt was made
        private String payment_method;
    }
}

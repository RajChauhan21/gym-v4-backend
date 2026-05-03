package com.backend.gym_backend.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SubscriptionActivatedEvent {

    private final Integer subscriptionId;

}

package com.backend.gym_backend.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class SubscriptionLinkedEvent {

    private final String razorpaySubscriptionId;
}

package com.backend.gym_backend.dto;

import lombok.Data;

@Data
public class VerifySubscriptionRequest{
    private String razorpayPaymentId;
    private String razorpaySubscriptionId;
    private String razorpaySignature;

}

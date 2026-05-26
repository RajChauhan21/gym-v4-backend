package com.backend.gym_backend.dto;

import lombok.Data;

@Data
public class PaymentDueResponse {

    private long dueAmount;
    private long dueMembersCount;

    public PaymentDueResponse(long dueAmount, long dueMembersCount) {
        this.dueAmount = dueAmount;
        this.dueMembersCount = dueMembersCount;

    }
}

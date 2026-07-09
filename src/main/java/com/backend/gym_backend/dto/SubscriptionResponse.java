package com.backend.gym_backend.dto;

import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.Plan;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SubscriptionResponse {

    private Integer id;

    private String name;

    private Integer price;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate billingDate;

    private Integer memberLimitCount;

    private SubscriptionStatus subscriptionStatus;

    private Owner owner;

    private Plan plan;
}

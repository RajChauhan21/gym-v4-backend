package com.backend.gym_backend.dto;

import com.backend.gym_backend.entity.PlanFeature;
import com.backend.gym_backend.entity.Subscription;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlanResponse {

    private Integer id;

    private String name;

    private Integer price;

    private String days;

    private Integer memberLimit;

    private List<Subscription> subscriptions;

    private List<PlanFeature> features;
}

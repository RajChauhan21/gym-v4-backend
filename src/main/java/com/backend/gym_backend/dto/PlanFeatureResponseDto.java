package com.backend.gym_backend.dto;

import com.backend.gym_backend.entity.Feature;
import com.backend.gym_backend.entity.Plan;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class PlanFeatureResponseDto {

    private Integer id;

    private Plan plan;

    private Feature feature;
}

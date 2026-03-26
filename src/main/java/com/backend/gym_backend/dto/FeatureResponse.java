package com.backend.gym_backend.dto;

import com.backend.gym_backend.entity.PlanFeature;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FeatureResponse {

    private Integer id;

    private String name;

    private String description;

    private List<PlanFeature> features;


}

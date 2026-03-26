package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlanRequest {

    private Integer id;

    private String name;

    private Integer price;

    private String days;

    private Integer memberLimit;
}

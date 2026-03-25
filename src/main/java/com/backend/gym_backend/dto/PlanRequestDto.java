package com.backend.gym_backend.dto;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlanRequestDto {

    private Integer id;

    private String name;

    private Integer price;

    private String days;

    private Integer memberLimit;
}

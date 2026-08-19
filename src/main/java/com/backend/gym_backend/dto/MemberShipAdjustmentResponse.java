package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MemberShipAdjustmentResponse {

    private int id;

    private LocalDate oldEndDate;

    private LocalDate newEndDate;

    private LocalDate freezeStartDate;

    private LocalDate freezeEndDate;

    private String notes;

    private String reason;

    private int durationDays;

    private int status;

    private int memberId;
}

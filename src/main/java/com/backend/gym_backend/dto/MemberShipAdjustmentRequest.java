package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MemberShipAdjustmentRequest {

    private int id;

    private int ownerId;

    private int memberId;

    private int durationDays;

    private String reason;

    private LocalDate oldEndDate;

    private LocalDate newEndDate;

    private LocalDate freezeStartDate;

    private LocalDate freezeEndDate;

    private String notes;

    private int status;
}

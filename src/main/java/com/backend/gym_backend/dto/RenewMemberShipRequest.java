package com.backend.gym_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@RequiredArgsConstructor
public class RenewMemberShipRequest {

    private Integer memberId;
    private Integer planId;
    private LocalDate joiningDate;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private Integer dueAmount;
}

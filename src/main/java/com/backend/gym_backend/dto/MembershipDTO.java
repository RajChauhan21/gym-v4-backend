package com.backend.gym_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MembershipDTO {

    private String planName;

    private String duration;

    private LocalDate startDate;

    private LocalDate expiryDate;

    private String trainerName;
}

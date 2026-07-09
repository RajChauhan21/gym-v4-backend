package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MemberExportDto {

    private Integer memberId;
    private String name;
    private String phone;
    private String email;
    private String source;
    private String membershipName;
    private LocalDate joinedDate;
    private Integer dueAmount;
    private LocalDate expiryDate;
    private Integer remainingDays;
    private String status;
    private LocalDate startDate;
}

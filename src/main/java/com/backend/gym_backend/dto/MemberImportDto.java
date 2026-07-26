package com.backend.gym_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberImportDto {

    private String name;

    private String phone;

    private String email;

    private String plan;

    private LocalDate joiningDate;

    private LocalDate startDate;

    private LocalDate expiryDate;

    private Integer dueAmount;

    private String address;

    private Integer excelRowNumber;

}

package com.backend.gym_backend.dto;

import com.backend.gym_backend.entity.MemberShip;
import com.backend.gym_backend.entity.Owner;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MemberResponse {
    private Integer id;

    private String name;

    private String email;

    private String phone;

    private String address;

    private LocalDate joined;

    private LocalDate expiry;

    private LocalDate startDate;

    private Integer dueAmount;

    private int ownerId;

    private int sourceId;

    private String plan;

}

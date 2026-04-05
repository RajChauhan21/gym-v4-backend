package com.backend.gym_backend.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberRequest {

    private Integer packageId;

    private Integer memberId;

    private Integer ownerId;

    private String name;

    private String email;

    private String phone;

    private String address;

    private LocalDate joined;

    private LocalDate expiry;

}

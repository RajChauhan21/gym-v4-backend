package com.backend.gym_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class MemberShipAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int memberId;

    private int ownerId;

    private int status; // 2 - freeze 3 - extended

    private int durationDays;

    private String reason;

    private String notes;

    private LocalDate oldEndDate;

    private LocalDate newEndDate;

    private LocalDate freezeStartDate;

    private LocalDate freezeEndDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private int isCurrent;
}

package com.backend.gym_backend.dto;

import java.time.LocalDate;

public interface MemberProjection {
    Integer getId();
    String getName();
    String getEmail();
    String getPhone();
    String getAddress();
    LocalDate getJoined();
    LocalDate getExpiry();
    Integer getDueAmount();
    int getOwnerId();
    String getPlan();
    LocalDate getStartDate();
    Integer getIsActive();
    String getSource();
}

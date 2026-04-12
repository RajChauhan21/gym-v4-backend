package com.backend.gym_backend.dto;

import java.time.LocalDate;

public interface MemberExpiryProjection {
    String getName();
    LocalDate getExpiry();
}

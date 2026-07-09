package com.backend.gym_backend.dto;

import java.math.BigDecimal;

public interface SourceAnalyticsProjection {

    Long getId();

    String getName();

    Long getTotalMembers();

    BigDecimal getTotalRevenue();
}
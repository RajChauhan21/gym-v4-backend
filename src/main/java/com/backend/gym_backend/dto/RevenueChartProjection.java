package com.backend.gym_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RevenueChartProjection {
    LocalDate getDate();

    BigDecimal getRevenue();
}

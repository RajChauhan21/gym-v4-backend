package com.backend.gym_backend.dto;

public interface RevenueProjection {

    Double getTotalRevenue();
    Double getCurrentMonthRevenue();
    Double getLastMonthRevenue();
    Long getActiveMemberCount();
    Long getActiveMembersThreeMonthsAgo();
    Long getNewMembersThisMonth();
    Long getNewMembersLastMonth();
    Long getExpiringSoonCount();
}

package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerDetailsRequest {

    private Integer ownerId;

    private String ownerName;

    private String email;

    private String phone;

    private String subscriptionPlan;

}

package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberSourceRequest {

    private Integer id;

    private Integer ownerId;

    private String name;
}

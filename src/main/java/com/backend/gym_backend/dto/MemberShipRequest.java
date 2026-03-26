package com.backend.gym_backend.dto;

import lombok.Data;

@Data
public class MemberShipRequest {

    private Integer id;

    private String name;

    private Integer validity;

    private Integer price;
}

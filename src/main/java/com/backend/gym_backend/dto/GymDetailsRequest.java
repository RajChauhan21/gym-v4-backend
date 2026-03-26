package com.backend.gym_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GymDetailsRequest {

    private Integer gymId;

    private Integer ownerId;

    private String name;

    private String website;

    private String location;

    private String googleMapUrl;
}

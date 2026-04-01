package com.backend.gym_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GymDetailsRequest {

    private Integer gymId;

    private Integer ownerId;

    private String gymName;

    private String website;

    private String ownerName;

    private String number;

    private String location;

    private String email;

    private String googleMapUrl;
}

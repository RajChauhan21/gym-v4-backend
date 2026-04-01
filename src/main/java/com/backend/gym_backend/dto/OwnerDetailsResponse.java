package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerDetailsResponse {

    private Integer ownerId;

    private Integer gymId;

    private String ownerName;

    private String gymName;

    private String email;

    private String phone;

    private String website;

    private String location;

    private String googleMapUrl;

    private String ownerImage;

    private String gymImage;
}

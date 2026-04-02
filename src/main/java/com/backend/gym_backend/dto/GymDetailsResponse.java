package com.backend.gym_backend.dto;

import com.backend.gym_backend.entity.Owner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GymDetailsResponse {

    private Integer gymId;

    private Integer ownerId;

    private String gymName;

    private String website;

    private String ownerName;

    private String number;

    private String location;

    private String email;

    private String googleMapUrl;

    private String ownerImage;

    private String gymImage;
}

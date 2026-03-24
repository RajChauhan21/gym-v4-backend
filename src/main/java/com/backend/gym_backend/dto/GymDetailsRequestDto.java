package com.backend.gym_backend.dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GymDetailsRequestDto {

    private Integer gymId;

    private Integer ownerId;

    private String name;

    private String website;

    private String location;

    private String googleMapUrl;
}

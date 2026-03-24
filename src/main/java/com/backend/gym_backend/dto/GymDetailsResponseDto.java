package com.backend.gym_backend.dto;

import com.backend.gym_backend.entity.Owner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GymDetailsResponseDto {

    private Integer id;

    private String name;

    private String website;

    private String location;

    private String googleMapUrl;

    private Owner owner;
}

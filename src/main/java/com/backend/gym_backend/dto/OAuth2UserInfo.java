package com.backend.gym_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2UserInfo {
    private String id;
    private String name;
    private String email;
    private String picture;
    private String provider;
}

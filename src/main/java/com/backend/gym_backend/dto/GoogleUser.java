package com.backend.gym_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleUser {
    private String sub; // The unique Google ID
    private String name;

    @JsonProperty("given_name")
    private String firstName;

    @JsonProperty("family_name")
    private String lastName;

    private String picture;
    private String email;

    @JsonProperty("email_verified")
    private boolean emailVerified;

}


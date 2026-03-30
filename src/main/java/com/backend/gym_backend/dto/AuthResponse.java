package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String username;
    private String password;
    private String token;
}

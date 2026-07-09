package com.backend.gym_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymDTO {

    private String name;

    private String address;

    private String phone;

    private String email;

    private String website;

    private String gstNumber;

    private String logoUrl;

    private String thankYouMessage;

    private String terms;

    private String signatureName;
}

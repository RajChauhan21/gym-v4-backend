package com.backend.gym_backend.dto;

import com.backend.gym_backend.enums.Subscription;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

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

    private String planName;

    private Integer price;

    private LocalDate startDate;

    private LocalDate endDate;

    private Subscription subscription;

    private Integer memberLimitCount;

    private Integer currentMemberCount;


}

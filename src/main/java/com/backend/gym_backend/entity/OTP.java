package com.backend.gym_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String ownerEmail;

    private String otp;

    private LocalDateTime sentAt;

    private LocalDateTime expiresAt;

    private Boolean isVerified;

    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;
}

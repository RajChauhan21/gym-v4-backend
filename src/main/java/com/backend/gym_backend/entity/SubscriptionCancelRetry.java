package com.backend.gym_backend.entity;

import com.backend.gym_backend.enums.Retry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class SubscriptionCancelRetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String razorpaySubscriptionId;

    private int retryCount;

    private LocalDateTime nextRetryAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Retry status; // PENDING, SUCCESS, FAILED

    private LocalDateTime createdAt;
}

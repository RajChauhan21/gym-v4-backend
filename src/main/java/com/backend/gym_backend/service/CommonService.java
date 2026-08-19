package com.backend.gym_backend.service;

import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class CommonService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public LocalDate convertEpochToLocalDate(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public Subscription checkSubscriptionOfOwner(Integer ownerId){
        return subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(ownerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE)).orElse(null);
    }
}

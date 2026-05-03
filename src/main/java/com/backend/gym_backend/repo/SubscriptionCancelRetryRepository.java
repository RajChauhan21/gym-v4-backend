package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.SubscriptionCancelRetry;
import com.backend.gym_backend.enums.Retry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionCancelRetryRepository extends JpaRepository<SubscriptionCancelRetry,Integer> {
    List<SubscriptionCancelRetry> findByStatusAndNextRetryAtBefore(Retry retry, LocalDateTime now);

}

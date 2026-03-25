package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRespository extends JpaRepository<Subscription,Integer> {
}

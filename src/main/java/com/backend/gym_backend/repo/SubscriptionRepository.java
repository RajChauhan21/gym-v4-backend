package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription,Integer> {

    List<Subscription> findByOwnerId(Integer ownerId);

    List<Subscription> findByPlanId(Integer planId);

    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

}

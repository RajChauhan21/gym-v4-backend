package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.enums.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription,Integer> {

    List<Subscription> findByOwnerId(Integer ownerId);

    List<Subscription> findByPlanId(Integer planId);

    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    Optional<List<Subscription>> findByOwnerAndStatus(Owner owner, SubscriptionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.razorpaySubscriptionId = :id")
    Optional<Subscription> findByRazorpaySubscriptionIdForUpdate(@Param("id") String id);


    Optional<Subscription> findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(Integer ownerId, SubscriptionStatus subscriptions);

}

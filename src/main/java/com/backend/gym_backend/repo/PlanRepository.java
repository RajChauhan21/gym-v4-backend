package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan,Integer> {

    Optional<Plan> findByName(String name);

    Optional<Plan> findByRazorPayPlanId(String planId);
}

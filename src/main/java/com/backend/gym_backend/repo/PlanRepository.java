package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan,Integer> {
}

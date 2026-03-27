package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.PlanFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanFeatureRepository extends JpaRepository<PlanFeature,Integer> {

    List<PlanFeature> findByPlanId(Integer planId);
}

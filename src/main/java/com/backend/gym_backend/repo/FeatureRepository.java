package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRepository extends JpaRepository<Feature,Integer> {
}

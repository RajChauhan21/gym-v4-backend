package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymRepository extends JpaRepository<Gym,Integer> {
}

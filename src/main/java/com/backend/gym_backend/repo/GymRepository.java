package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GymRepository extends JpaRepository<Gym,Integer> {

    Optional<Gym> findByName(String gymName);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);
}

package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner,Integer> {

    Optional<Owner> findByEmail(String email);
}

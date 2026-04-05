package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.MemberShip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberShipRepository extends JpaRepository<MemberShip,Integer> {


    Optional<MemberShip> findByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
}

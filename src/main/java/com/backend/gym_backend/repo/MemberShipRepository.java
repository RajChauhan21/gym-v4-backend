package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.MemberShip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberShipRepository extends JpaRepository<MemberShip,Integer> {
}

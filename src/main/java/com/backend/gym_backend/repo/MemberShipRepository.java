package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.MemberShip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberShipRepository extends JpaRepository<MemberShip,Integer> {


    Optional<MemberShip> findByName(String name);
    boolean existsByNameAndGymIdAndIdNot(String name, Integer gymId, Integer id);

    List<MemberShip> findAllByGymId(Integer id);

    boolean existsByNameIgnoreCaseAndGymId(String name, Integer gymId);
    Optional<MemberShip> findByNameIgnoreCaseAndGymId(String name, Integer gymId);

}

package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.MemberShip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberShipRepository extends JpaRepository<MemberShip,Integer> {


    Optional<MemberShip> findByName(String name);
    boolean existsByNameAndGymIdAndIdNot(String name, Integer gymId, Integer id);

    List<MemberShip> findAllByGymId(Integer id);

    boolean existsByNameIgnoreCaseAndGymId(String name, Integer gymId);
    Optional<MemberShip> findByNameIgnoreCaseAndGymId(String name, Integer gymId);

    @Query(value = """
        SELECT
            ms.id AS membershipId,
            ms.name AS membershipName,
            ms.price AS price,
            ms.validity AS validity,
            COUNT(m.id) AS memberCount
        FROM member_ship ms
        LEFT JOIN member m
            ON m.member_ship_id = ms.id
        LEFT JOIN owner o
            ON o.id = m.owner_id
            AND o.gym_id = :gymId
        WHERE ms.gym_id = :gymId
        GROUP BY ms.id, ms.name, ms.price, ms.validity
        """, nativeQuery = true)
    List<Object[]> findAllPlansWithMemberCount(
            @Param("gymId") Integer gymId
    );

}

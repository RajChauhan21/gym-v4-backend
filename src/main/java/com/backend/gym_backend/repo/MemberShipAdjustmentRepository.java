package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.MemberShipAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MemberShipAdjustmentRepository extends JpaRepository<MemberShipAdjustment, Integer> {
    Optional<List<MemberShipAdjustment>> findByMemberIdAndIsCurrent(Integer id,int isCurrent);

    Optional<MemberShipAdjustment> findByMemberIdAndStatus(Integer id, Integer status);

    @Query("""
            SELECT m.memberId
            FROM MemberShipAdjustment m
            WHERE m.memberId IN :memberIds
              AND m.status = :status
              AND m.newEndDate >= :today
        """)
    Set<Integer> findActiveExtensionMemberIds(
            @Param("memberIds") Set<Integer> memberIds,
            @Param("status") int status,
            @Param("today") LocalDate today
    );

    @Query("""
                SELECT m
                FROM MemberShipAdjustment m
                WHERE
                    (m.status = 2 AND m.freezeEndDate < :today)
                    OR
                    (m.status = 3 AND m.newEndDate < :today)
            """)
    List<MemberShipAdjustment> findExpiredAdjustments(
            @Param("today") LocalDate today
    );

}

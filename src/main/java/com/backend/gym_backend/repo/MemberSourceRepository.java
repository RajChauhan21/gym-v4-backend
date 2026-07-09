package com.backend.gym_backend.repo;

import com.backend.gym_backend.dto.SourceAnalyticsProjection;
import com.backend.gym_backend.entity.MemberSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberSourceRepository extends JpaRepository<MemberSource, Integer> {

    List<MemberSource> findAllByOwnerId(Integer ownerId);

    boolean existsByNameAndOwnerId(String name, Integer ownerId);

    @Query(value = """
            SELECT
                s.id AS id,
                s.name AS name,
                COUNT(DISTINCT m.id) AS totalMembers,
                COALESCE(SUM(pp.total_paid), 0) AS totalRevenue
            FROM member_source s
            LEFT JOIN member m
                   ON m.source_id = s.id
            LEFT JOIN (
                SELECT
                    member_id,
                    SUM(amount_paid) AS total_paid
                FROM payment
                GROUP BY member_id
            ) pp
                   ON pp.member_id = m.id
            WHERE s.owner_id = :ownerId
            GROUP BY s.id, s.name
            ORDER BY totalMembers DESC
            """, nativeQuery = true)
    List<SourceAnalyticsProjection> findSourceAnalytics(Integer ownerId);

    @Query(value = """
                select COUNT(*)
                FROM member_source s
                WHERE s.owner_id = :ownerId
            """, nativeQuery = true)
    long countSourcesByOwnerId(@Param("ownerId") Integer ownerId);


    @Query(value = """
                SELECT count(*)
                FROM member m
                WHERE m.source_id = :sourceId
            """, nativeQuery = true)
    Integer countMembersBySourceId(@Param("sourceId") Integer sourceId);

    boolean existsByNameIgnoreCaseAndOwnerIdAndIdNot(String strip, Integer ownerId, Integer id);

}

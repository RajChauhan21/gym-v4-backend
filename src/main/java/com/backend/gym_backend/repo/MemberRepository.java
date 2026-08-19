package com.backend.gym_backend.repo;

import com.backend.gym_backend.dto.MemberExpiryProjection;
import com.backend.gym_backend.dto.MemberProjection;
import com.backend.gym_backend.dto.MemberSearchProjection;
import com.backend.gym_backend.dto.RevenueProjection;
import com.backend.gym_backend.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    List<Member> findByOwnerId(Integer ownerId);

    long countByMemberShipId(Integer memberShipId);

    @Query(value = "SELECT COUNT(*) FROM member m " +
            "JOIN member_ship ms ON m.member_ship_id = ms.id " +
            "WHERE m.member_ship_id = :membershipId AND ms.gym_id = :gymId AND m.owner_id = " + ":ownerId",
            nativeQuery = true)
    long countByMembershipIdAndGymId(@Param("membershipId") Integer membershipId, @Param("gymId") Integer gymId, @Param("ownerId") Integer ownerId);

    boolean existsByNameAndOwnerIdAndIdNot(String name, Integer ownerId, Integer id);

    boolean existsByNameAndOwnerId(String name, Integer ownerId);

    @Query(
            value = "SELECT m.id AS id, m.name AS name, m.email AS email, m.phone AS phone, " +
                    "m.address AS address, m.start_date AS startDate, m.joined AS joined, m.expiry AS expiry, " +
                    "m.due_amount AS dueAmount, m.is_active AS isActive, m.owner_id AS ownerId, " +
                    "ms.name AS plan, src.name AS source " +
                    "FROM member m " +
                    "LEFT JOIN member_ship ms ON m.member_ship_id = ms.id " +
                    "LEFT JOIN member_source src ON m.source_id = src.id AND src.owner_id = m.owner_id " +
                    "WHERE m.owner_id = ?1 " +
                    "AND (?2 IS NULL OR m.name LIKE CONCAT('%', ?2, '%')) " +
                    "AND (?3 IS NULL OR CAST(m.due_amount AS CHAR) LIKE CONCAT('%', ?3, '%')) " +
                    "AND (?4 IS NULL OR m.source_id = ?4) " +
                    "AND (?5 IS NULL OR m.joined >= ?5) " +
                    "AND (?6 IS NULL OR m.joined <= ?6) " +
                    "AND (?7 IS NULL OR m.expiry >= ?7) " +
                    "AND (?8 IS NULL OR m.expiry <= ?8) " +
                    "AND (?9 IS NULL OR m.start_date >= ?9) " +
                    "AND (?10 IS NULL OR m.start_date <= ?10) " +
                    "AND (?11 IS NULL OR ms.name = ?11) " +
                    "AND (?12 IS NULL OR m.is_active = ?12)",
            countQuery = "SELECT COUNT(*) FROM member m " +
                    "LEFT JOIN member_ship ms ON m.member_ship_id = ms.id " +
                    "LEFT JOIN member_source src ON m.source_id = src.id AND src.owner_id = m.owner_id " +
                    "WHERE m.owner_id = ?1 " +
                    "AND (?2 IS NULL OR m.name LIKE CONCAT('%', ?2, '%')) " +
                    "AND (?3 IS NULL OR CAST(m.due_amount AS CHAR) LIKE CONCAT('%', ?3, '%')) " +
                    "AND (?4 IS NULL OR m.source_id = ?4) " +
                    "AND (?5 IS NULL OR m.joined >= ?5) " +
                    "AND (?6 IS NULL OR m.joined <= ?6) " +
                    "AND (?7 IS NULL OR m.expiry >= ?7) " +
                    "AND (?8 IS NULL OR m.expiry <= ?8) " +
                    "AND (?9 IS NULL OR m.start_date >= ?9) " +
                    "AND (?10 IS NULL OR m.start_date <= ?10) " +
                    "AND (?11 IS NULL OR ms.name = ?11) " +
                    "AND (?12 IS NULL OR m.is_active = ?12)",
            nativeQuery = true
    )
    Page<MemberProjection> findAllMembersByOwnerId(
            Long ownerId,         // ?1
            String name,          // ?2
            String dueAmount,     // ?3
            Integer sourceId,     // ?4
            LocalDate joinedFrom, // ?5
            LocalDate joinedTo,   // ?6
            LocalDate expiryFrom, // ?7
            LocalDate expiryTo,   // ?8
            LocalDate startDateFrom, // ?9
            LocalDate startDateTo, // ?10
            String plan,          // ?11
            Integer isActive,     // ?12
            Pageable pageable
    );

    @Query(
            value = "SELECT COUNT(*) FROM member m " +
                    "LEFT JOIN member_ship ms ON m.member_ship_id = ms.id " +
                    "WHERE m.owner_id = ?1 " +
                    "AND (?2 IS NULL OR CAST(m.due_amount AS CHAR) LIKE CONCAT('%', ?2, '%')) " +
                    "AND (?3 IS NULL OR m.joined >= ?3) " +
                    "AND (?4 IS NULL OR m.joined <= ?4) " +
                    "AND (?5 IS NULL OR m.expiry >= ?5) " +
                    "AND (?6 IS NULL OR m.expiry <= ?6) " +
                    "AND (?7 IS NULL OR m.start_date >= ?7) " +
                    "AND (?8 IS NULL OR m.start_date <= ?8) " +
                    "AND (?9 IS NULL OR ms.name = ?9) " +
                    "AND (?10 IS NULL OR m.is_active = ?10)",
            nativeQuery = true
    )
    long countMembersByOwnerIdAndFilters(
            Long ownerId,            // ?1
            String dueAmount,       // ?2
            LocalDate joinedFrom,    // ?3
            LocalDate joinedTo,      // ?4
            LocalDate expiryFrom,    // ?5
            LocalDate expiryTo,      // ?6
            LocalDate startDateFrom, // ?7
            LocalDate startDateTo,   // ?8
            String plan,             // ?9
            Integer isActive         // ?10
    );


    @Query(value = "SELECT COUNT(*) FROM member WHERE owner_id = :ownerId", nativeQuery = true)
    Integer countAllMembersByOwnerId(@Param("ownerId") Integer ownerId);

    @Query(value = "SELECT COUNT(*) FROM member WHERE owner_id = :ownerId and is_active = :isActive", nativeQuery = true)
    Integer countAllMembersByOwnerIdAndIsActive(@Param("ownerId") Integer ownerId, @Param("isActive") Integer isActive);


    @Query(value = """
            SELECT
                m.id AS memberId,
                m.name AS fullName,
                mm.name AS planName
            FROM member m
            LEFT JOIN member_ship mm ON mm.id = m.member_ship_id
            WHERE m.owner_id = :ownerId
              AND LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY m.name
            LIMIT 10
            """, nativeQuery = true)
    List<MemberSearchProjection> searchMembers(
            @Param("ownerId") Integer ownerId,
            @Param("query") String query
    );

    @Query(value = "SELECT COUNT(*) FROM member WHERE owner_id = :ownerId AND expiry > CURDATE()",
            nativeQuery = true)
    Integer countActiveMembersByOwner(@Param("ownerId") Integer ownerId);


    @Query(value = "SELECT COUNT(*) FROM member " +
            "WHERE owner_id = :ownerId " +
            "AND MONTH(joined) = MONTH(CURDATE()) " +
            "AND YEAR(joined) = YEAR(CURDATE())",
            nativeQuery = true)
    Integer countNewMembersThisMonth(@Param("ownerId") Integer ownerId);

    @Query(value = "SELECT COUNT(*) FROM member " +
            "WHERE owner_id = :ownerId " +
            "AND expiry BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)",
            nativeQuery = true)
    Integer countMembersExpiringSoon(@Param("ownerId") Integer ownerId);

    @Query(value = "SELECT " +
            "  SUM(p.amount_paid) AS totalRevenue, " +
            "  SUM(CASE WHEN MONTH(p.date) = MONTH(CURDATE()) AND YEAR(p.date) = YEAR(CURDATE()) THEN p.amount_paid ELSE 0 END) AS currentMonthRevenue, " +
            "  SUM(CASE WHEN MONTH(p.date) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) AND YEAR(p.date) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) THEN p.amount_paid ELSE 0 END) AS lastMonthRevenue, " +
            "  COUNT(DISTINCT CASE WHEN m.expiry > CURDATE() THEN m.id END) AS activeMemberCount, " +
            // Fixed: This will now count the 6th member even if they have 0 payments
            "COUNT(DISTINCT CASE WHEN MONTH(m.joined) = MONTH(DATE_SUB(CURDATE(), INTERVAL 3 MONTH)) AND YEAR(m.joined) = YEAR(DATE_SUB(CURDATE(), INTERVAL 3 MONTH)) THEN m.id END) AS activeMembersThreeMonthsAgo, " +
            "  COUNT(DISTINCT CASE WHEN MONTH(m.joined) = MONTH(CURDATE()) AND YEAR(m.joined) = YEAR(CURDATE()) THEN m.id END) AS newMembersThisMonth, " +
            "  COUNT(DISTINCT CASE WHEN MONTH(m.joined) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) AND YEAR(m.joined) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) THEN m.id END) AS newMembersLastMonth, " +
            "  COUNT(DISTINCT CASE WHEN m.expiry BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) THEN m.id END) AS expiringSoonCount " +
            "FROM member m " + // 1. Start with the MEMBER table
            "LEFT JOIN payment p ON p.member_id = m.id " + // 2. LEFT JOIN to include members with 0 payments
            "WHERE m.owner_id = :ownerId",
            nativeQuery = true)
    RevenueProjection getFullStatsByOwner(@Param("ownerId") Integer ownerId);


    @Query(value = """
            SELECT
                m.name AS name,
                m.expiry AS expiry
            FROM member m
            WHERE m.owner_id = :ownerId
              AND m.expiry BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL 7 DAY)
            ORDER BY m.expiry ASC
            """, nativeQuery = true)
    List<MemberExpiryProjection> findExpiringMembers(@Param("ownerId") Integer ownerId);

    @Modifying
    @Query("""
                UPDATE Member m
                SET m.isActive = :inactiveStatus,
                    m.updatedAt = :updatedAt
                WHERE m.expiry IS NOT NULL
                  AND m.expiry < :today
                  AND m.isActive <> :inactiveStatus
            """)
    int updateExpiredMembersToInactive(
            @Param("today") LocalDate today,
            @Param("inactiveStatus") int inactiveStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );

}

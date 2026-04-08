package com.backend.gym_backend.repo;

import com.backend.gym_backend.dto.MemberProjection;
import com.backend.gym_backend.dto.MemberSearchProjection;
import com.backend.gym_backend.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    List<Member> findByOwnerId(Integer ownerId);

    List<Member> findByMemberShipId(Integer memberShipId);

    boolean existsByNameAndIdNot(String name, Integer id);

    boolean existsByName(String name);

    @Query(
            value = "SELECT m.id AS id, m.name AS name, m.email AS email, m.phone AS phone, " +
                    "m.address AS address, m.joined AS joined, m.expiry AS expiry, " +
                    "m.due_amount AS dueAmount, m.owner_id AS ownerId, ms.name AS plan " +
                    "FROM member m " +
                    "LEFT JOIN member_ship ms ON m.member_ship_id = ms.id " +
                    "WHERE m.owner_id = ?1 " +
                    "AND (?2 IS NULL OR m.name LIKE CONCAT('%', ?2, '%')) " +
                    "AND (?3 IS NULL OR m.due_amount = ?3) " +
                    "AND (?4 IS NULL OR m.joined >= ?4) " +
                    "AND (?5 IS NULL OR m.joined <= ?5) " +
                    "AND (?6 IS NULL OR m.expiry >= ?6) " +
                    "AND (?7 IS NULL OR m.expiry <= ?7) " +
                    "AND (?8 IS NULL OR ms.name = ?8)", // New Plan Filter
            countQuery = "SELECT COUNT(*) FROM member m " +
                    "LEFT JOIN member_ship ms ON m.member_ship_id = ms.id " + // Join needed for count too
                    "WHERE m.owner_id = ?1 " +
                    "AND (?2 IS NULL OR m.name LIKE CONCAT('%', ?2, '%')) " +
                    "AND (?3 IS NULL OR m.due_amount = ?3) " +
                    "AND (?4 IS NULL OR m.joined >= ?4) " +
                    "AND (?5 IS NULL OR m.joined <= ?5) " +
                    "AND (?6 IS NULL OR m.expiry >= ?6) " +
                    "AND (?7 IS NULL OR m.expiry <= ?7) " +
                    "AND (?8 IS NULL OR ms.name = ?8)",
            nativeQuery = true
    )
    Page<MemberProjection> findAllMembersByOwnerId(
            Long ownerId,     // ?1
            String name,      // ?2
            Integer dueAmount,// ?3
            LocalDate joinedFrom, // ?4
            LocalDate joinedTo,   // ?5
            LocalDate expiryFrom, // ?6
            LocalDate expiryTo,   // ?7
            String plan,      // ?8
            Pageable pageable
    );

    @Query(value = "SELECT COUNT(*) FROM member WHERE owner_id = :ownerId", nativeQuery = true)
    Integer countAllMembersByOwnerId(@Param("ownerId") Integer ownerId);


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


}

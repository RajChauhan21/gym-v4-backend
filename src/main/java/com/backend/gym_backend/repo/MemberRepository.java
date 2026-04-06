package com.backend.gym_backend.repo;

import com.backend.gym_backend.dto.MemberProjection;
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
            value = """
                    SELECT 
                        m.id AS id, m.name AS name, m.email AS email, m.phone AS phone, 
                        m.address AS address, m.joined AS joined, m.expiry AS expiry, 
                        m.due_amount AS dueAmount, m.owner_id AS ownerId, ms.name AS plan 
                    FROM member m 
                    LEFT JOIN member_ship ms ON m.member_ship_id = ms.id 
                    WHERE m.owner_id = :ownerId
                    AND (:name IS NULL OR m.name LIKE %:name%)
                    AND (:email IS NULL OR m.email LIKE %:email%)
                    AND (:dueAmount IS NULL OR m.due_amount = :dueAmount)
                    -- Date Range Logic for Joined
                    AND (:joinedFrom IS NULL OR m.joined >= :joinedFrom)
                    AND (:joinedTo IS NULL OR m.joined <= :joinedTo)
                    -- Date Range Logic for Expiry
                    AND (:expiryFrom IS NULL OR m.expiry >= :expiryFrom)
                    AND (:expiryTo IS NULL OR m.expiry <= :expiryTo)
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM member m 
                    WHERE m.owner_id = :ownerId
                    AND (:name IS NULL OR m.name LIKE %:name%)
                    -- ... include all other filters from above for accurate counting ...
                    AND (:joinedFrom IS NULL OR m.joined >= :joinedFrom)
                    AND (:joinedTo IS NULL OR m.joined <= :joinedTo)
                    AND (:expiryFrom IS NULL OR m.expiry >= :expiryFrom)
                    AND (:expiryTo IS NULL OR m.expiry <= :expiryTo)
                    """,
            nativeQuery = true
    )
    Page<MemberProjection> findMembersFiltered(
            @Param("ownerId") Long ownerId,
            @Param("name") String name,
            @Param("email") String email,
            @Param("dueAmount") Integer dueAmount,
            @Param("joinedFrom") LocalDate joinedFrom,
            @Param("joinedTo") LocalDate joinedTo,
            @Param("expiryFrom") LocalDate expiryFrom,
            @Param("expiryTo") LocalDate expiryTo,
            Pageable pageable
    );


}

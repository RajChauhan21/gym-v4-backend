package com.backend.gym_backend.repo;

import com.backend.gym_backend.dto.PaymentProjection;
import com.backend.gym_backend.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query(
            value = """
                SELECT
                    p.id AS paymentId,
                    p.member_id AS memberId,
                    m.name AS memberName,
                    ms.name AS membershipName,
                    p.amount_paid AS amount,
                    p.method AS method,
                    p.date AS paymentDate
                FROM payment p
                JOIN member m ON p.member_id = m.id
                JOIN member_ship ms ON m.member_ship_id = ms.id
                WHERE m.owner_id = :ownerId
            """,
            countQuery = """
                SELECT COUNT(*)
                FROM payment p
                JOIN member m ON p.member_id = m.id
                JOIN member_ship ms ON m.member_ship_id = ms.id
                WHERE m.owner_id = :ownerId
            """,
            nativeQuery = true
    )
    Page<PaymentProjection> findPaymentsByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);



}

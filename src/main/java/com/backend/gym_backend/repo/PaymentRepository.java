package com.backend.gym_backend.repo;

import com.backend.gym_backend.dto.PaymentProjection;
import com.backend.gym_backend.dto.RecentPaymentProjection;
import com.backend.gym_backend.dto.RevenueChartProjection;
import com.backend.gym_backend.dto.RevenueProjection;
import com.backend.gym_backend.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

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
                    AND (:memberName IS NULL OR m.name LIKE %:memberName%)
                    AND (:membershipName IS NULL OR ms.name LIKE %:membershipName%)
                    AND (:method IS NULL OR p.method = :method)
                    AND (:amount IS NULL OR p.amount_paid = :amount)
                    -- Date Range Logic for Payment Date
                    AND (:dateFrom IS NULL OR p.date >= :dateFrom)
                    AND (:dateTo IS NULL OR p.date <= :dateTo)
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM payment p
                    JOIN member m ON p.member_id = m.id
                    JOIN member_ship ms ON m.member_ship_id = ms.id
                    WHERE m.owner_id = :ownerId
                    AND (:memberName IS NULL OR m.name LIKE %:memberName%)
                    AND (:membershipName IS NULL OR ms.name LIKE %:membershipName%)
                    AND (:method IS NULL OR p.method = :method)
                    AND (:amount IS NULL OR p.amount_paid = :amount)
                    AND (:dateFrom IS NULL OR p.date >= :dateFrom)
                    AND (:dateTo IS NULL OR p.date <= :dateTo)
                    """,
            nativeQuery = true
    )
    Page<PaymentProjection> findPaymentsFiltered(
            @Param("ownerId") Long ownerId,
            @Param("memberName") String memberName,
            @Param("membershipName") String membershipName,
            @Param("method") String method,
            @Param("amount") Integer amount,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );


    @Query(value = "SELECT COALESCE(SUM(amount_paid), 0) FROM payment", nativeQuery = true)
    Long sumAllAmounts();

    @Query(value = "SELECT SUM(amount_paid) FROM payment " +
            "WHERE MONTH(date) = MONTH(CURDATE()) " +
            "AND YEAR(date) = YEAR(CURDATE())",
            nativeQuery = true)
    Double calculateCurrentMonthRevenueNative();

    @Query(value = "SELECT " +
            "  SUM(p.amount_paid) AS totalRevenue, " +
            "  SUM(CASE WHEN MONTH(p.date) = MONTH(CURDATE()) " +
            "            AND YEAR(p.date) = YEAR(CURDATE()) " +
            "       THEN p.amount_paid ELSE 0 END) AS currentMonthRevenue, " +
            "  COUNT(p.id) AS totalRecords " +
            "FROM payment p " +
            "JOIN member m ON p.member_id = m.id " +
            "WHERE m.owner_id = :ownerId",
            nativeQuery = true)
    RevenueProjection getRevenueByOwner(@Param("ownerId") Integer ownerId);


    @Query(value = """
            SELECT
                                     DATE(p.date) AS date,
                                     COALESCE(SUM(p.amount_paid),0) AS revenue
                                 FROM payment p
                                 JOIN member m ON m.id = p.member_id
                                 WHERE m.owner_id = :ownerId
                                 AND DATE(p.date)
                                 BETWEEN DATE_SUB(CURDATE(), INTERVAL (:days - 1) DAY)
                                 AND CURDATE()
                                 GROUP BY DATE(p.date)
                                 ORDER BY DATE(p.date);
            """, nativeQuery = true)
    List<RevenueChartProjection> getRevenueOverview(Integer ownerId, Integer days);


    @Query(value = """
        SELECT
            m.name AS memberName,
            p.amount_paid AS amount
        FROM payment p
        INNER JOIN member m ON p.member_id = m.id
        WHERE m.owner_id = :ownerId
        ORDER BY p.date DESC
        LIMIT 5
        """, nativeQuery = true)
    List<RecentPaymentProjection> findRecentPaymentsByOwner(@Param("ownerId") Integer ownerId);


}

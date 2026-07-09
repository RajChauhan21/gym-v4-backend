package com.backend.gym_backend.repo;

import com.backend.gym_backend.dto.OwnerPaymentProjection;
import com.backend.gym_backend.entity.Invoice;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.OwnerPayment;
import com.backend.gym_backend.enums.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OwnerPaymentRepository extends JpaRepository<OwnerPayment, Integer> {


    Optional<OwnerPayment> findByRazorpayPaymentId(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM OwnerPayment p WHERE p.razorpayPaymentId = :id")
    Optional<OwnerPayment> findByRazorpayPaymentIdForUpdate(@Param("id") String id);

    List<OwnerPayment> findTop50BySubscriptionIsNullOrderByCreatedAtAsc();

    List<OwnerPayment> findByInvoiceAndSubscriptionIsNull(Invoice invoice);

    Page<OwnerPayment> findByOwner(Owner ownerId, Pageable pageable);

    @Query("""
                SELECT
                    p.amount AS amount,
                    p.status AS status,
                    p.method AS method,
                    p.createdAt AS createdAt,
                    i.invoiceUrl AS invoiceUrl
                FROM OwnerPayment p
                LEFT JOIN p.invoice i
                WHERE p.owner.id = :ownerId
            
                AND (:amount IS NULL OR CAST(p.amount AS string) LIKE :amount)
            
                AND (:status IS NULL OR p.status = :status)
            
                AND (:method IS NULL OR p.method = :method)
            
                AND (:startDate IS NULL OR p.createdAt >= :startDate)
            
                AND (:endDate IS NULL OR p.createdAt <= :endDate)
            """)
    Page<OwnerPaymentProjection> findPaymentsByOwner(
            @Param("ownerId") Integer ownerId,
            @Param("amount") String amount,
            @Param("status") Payment status,
            @Param("method") String method,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
                SELECT
                    COUNT(p)
                FROM OwnerPayment p
                LEFT JOIN p.invoice i
                WHERE p.owner.id = :ownerId
            
                AND (:amount IS NULL OR CAST(p.amount AS string) LIKE :amount)
            
                AND (:status IS NULL OR p.status = :status)
            
                AND (:method IS NULL OR p.method = :method)
            
                AND (:startDate IS NULL OR p.createdAt >= :startDate)
            
                AND (:endDate IS NULL OR p.createdAt <= :endDate)
            """)
    Long countPaymentsByOwner(
            @Param("ownerId") Integer ownerId,
            @Param("amount") String amount,
            @Param("status") Payment status,
            @Param("method") String method,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}

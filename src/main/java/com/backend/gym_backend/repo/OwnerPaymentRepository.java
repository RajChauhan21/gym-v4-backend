package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Invoice;
import com.backend.gym_backend.entity.OwnerPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OwnerPaymentRepository extends JpaRepository<OwnerPayment,Integer> {


    Optional<OwnerPayment> findByRazorpayPaymentId(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM OwnerPayment p WHERE p.razorpayPaymentId = :id")
    Optional<OwnerPayment> findByRazorpayPaymentIdForUpdate(@Param("id") String id);

    List<OwnerPayment> findTop50BySubscriptionIsNullOrderByCreatedAtAsc();

    List<OwnerPayment> findByInvoiceAndSubscriptionIsNull(Invoice invoice);

}

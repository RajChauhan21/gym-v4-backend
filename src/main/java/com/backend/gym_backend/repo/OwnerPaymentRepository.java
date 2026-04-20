package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.OwnerPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerPaymentRepository extends JpaRepository<OwnerPayment,Integer> {


    Optional<OwnerPayment> findByRazorpayPaymentId(String id);
}

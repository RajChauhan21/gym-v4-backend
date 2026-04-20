package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.OwnerPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerPaymentRepository extends JpaRepository<OwnerPayment,Integer> {
}

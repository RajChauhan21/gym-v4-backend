package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice,Integer> {


    Optional<Invoice> findByRazorpayInvoiceId(String id);
}

package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.OTP;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OTP,Integer> {

    Optional<OTP> findByOwnerEmail(String email);

    @Modifying
    @Transactional
    void deleteByOwnerEmail(String toEmail);
}

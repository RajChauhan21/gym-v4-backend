package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {


    Optional<RefreshToken> findByToken(String token);

    @Transactional
    void deleteByOwnerId(Integer ownerId);

    Optional<RefreshToken> findByOwnerId(Integer ownerId);

    @Transactional
    void deleteByToken(String token);

    Optional<RefreshToken> findByOwner(Owner owner);
}

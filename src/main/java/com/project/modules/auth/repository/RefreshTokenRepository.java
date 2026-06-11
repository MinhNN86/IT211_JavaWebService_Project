package com.project.modules.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.modules.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("update RefreshToken token set token.revoked = true where token.user.id = :userId")
    void revokeByUserId(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}

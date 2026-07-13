package com.fooddelivery.repository;

import com.fooddelivery.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Delete all tokens for a user before issuing a new one */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteByUserId(Long userId);

    /** Cleanup job-friendly: delete all expired tokens */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}

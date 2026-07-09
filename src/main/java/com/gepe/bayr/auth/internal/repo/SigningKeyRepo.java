package com.gepe.bayr.auth.internal.repo;

import com.gepe.bayr.auth.internal.entity.SigningKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SigningKeyRepo extends JpaRepository<SigningKey, Long> {
    Optional<SigningKey> findByKid(String kid);

    Optional<SigningKey> findTopByIsActiveTrueOrderByCreatedAtDesc();

    long countByIsActiveTrue();

    @Modifying
    @Query("UPDATE SigningKey k SET k.isActive = false WHERE k.isActive = true")
    void deactivateAllActiveKeys();

    /** Returns keys that are still active OR haven't expired yet (for JWKS endpoint) */
    @Query("SELECT k FROM SigningKey k WHERE k.isActive = true OR k.expiresAt > :now")
    List<SigningKey> findAllNotExpiredOrActive(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM SigningKey k WHERE k.isActive = false AND k.expiresAt < :now")
    int deleteExpiredKeys(@Param("now") Instant now);
}

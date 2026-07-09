package com.gepe.bayr.auth.internal.repo;

import com.gepe.bayr.auth.internal.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshSessionRepo extends JpaRepository<RefreshSession, Long> {

    Optional<RefreshSession> findBySessionId(UUID sessionId);

    @Modifying
    @Query("""
            UPDATE RefreshSession s
            SET s.isRevoked = true, s.revokedAt = CURRENT_TIMESTAMP, s.revokeReason = :reason
            WHERE s.userId = :userId AND s.isRevoked = false
            """)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("reason") String reason);

    /**
     * Cleanup job: remove expired + revoked sessions older than 30 days
     */
    @Modifying
    @Query("DELETE FROM RefreshSession s WHERE s.expiresAt < :cutoff AND s.isRevoked = true")
    int deleteExpiredSessions(@Param("cutoff") Instant cutoff);
}

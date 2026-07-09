package com.gepe.bayr.auditLogs.internal.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
@RequiredArgsConstructor
public class AuditLogArchiveRepositoryJdbc {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int archive(Instant cutoff, int batchSize) {
        OffsetDateTime cutoffOdt = cutoff.atOffset(ZoneOffset.UTC);
        Integer archived = jdbcTemplate.queryForObject("""
                        WITH moved_rows AS (
                            DELETE FROM audit_logs
                            WHERE id IN (
                                SELECT id
                                FROM audit_logs
                                WHERE created_at < ?
                                ORDER BY created_at
                                LIMIT ?
                            )
                            RETURNING *
                        ),
                        inserted AS (
                            INSERT INTO audit_logs_archive
                            SELECT *
                            FROM moved_rows
                            RETURNING id
                        )
                        SELECT COUNT(*)
                        FROM inserted
                        """,
                Integer.class,
                cutoffOdt,
                batchSize
        );

        return archived == null ? 0 : archived;
    }
}

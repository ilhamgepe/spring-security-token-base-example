package com.gepe.bayr.auditLogs.internal.service;


import com.gepe.bayr.auditLogs.internal.repo.AuditLogArchiveRepositoryJdbc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogArchiveService {
    private final AuditLogArchiveRepositoryJdbc auditLogArchiveRepositoryJdbc;
    private static final int BATCH_SIZE = 10_000;

    // pakr trx nya di repository agar kalo banyak tidak lama transactionya karna kalo di repo, selesai commit selesai commit. kalo di service jadi nunggu semua baru komit
    public int archiveOlderThan(Instant cutoff) {
        int totalArchived = 0;

        while (true) {
            int archived = auditLogArchiveRepositoryJdbc.archive(cutoff, BATCH_SIZE);

            if (archived == 0) break;

            totalArchived += archived;
            log.info("Archived {} audit logs", archived);

        }
        return totalArchived;
    }
}

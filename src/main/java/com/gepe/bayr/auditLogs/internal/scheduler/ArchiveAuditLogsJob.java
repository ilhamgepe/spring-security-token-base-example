package com.gepe.bayr.auditLogs.internal.scheduler;


import com.gepe.bayr.auditLogs.internal.service.AuditLogArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@DisallowConcurrentExecution // never run two rotations in parallel
@RequiredArgsConstructor
public class ArchiveAuditLogsJob implements Job {
    private final AuditLogArchiveService auditLogArchiveService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        try {
            log.info(
                    "Starting audit log archival. Cutoff={}",
                    cutoff
            );
            int totalArchived =
                    auditLogArchiveService.archiveOlderThan(cutoff);
            log.info(
                    "Audit log archival completed. Archived {} records",
                    totalArchived
            );
        } catch (Exception e) {
            log.error(
                    "Audit log archival failed",
                    e
            );

            throw new JobExecutionException(e);
        }

    }
}

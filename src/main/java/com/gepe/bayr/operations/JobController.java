package com.gepe.bayr.operations;

import com.gepe.bayr.shared.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/operations/jobs")
public class JobController {
    private final Scheduler scheduler;

    @PostMapping("/auditLogs/archive")
    public ResponseEntity<ApiResponse<String>> triggerArchiveAuditLogsJob() {
        try {
            JobKey jobKey = new JobKey("archiveAuditLogsJob", "auditLogs");
            if (!scheduler.checkExists(jobKey)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("Job detail tidak ditemukan di Quartz Scheduler", null));
            }

            scheduler.triggerJob(jobKey);
            return ResponseEntity.ok(new ApiResponse<>("Archive old audit logs job triggered", null));
        } catch (SchedulerException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(e.getMessage(), "failed to archive audit logs job"));
        }
    }

    @PostMapping("/signing-keys/rotate")
    public ResponseEntity<ApiResponse<String>> forceKeyRotation() {
        try {
            JobKey jobKey = new JobKey("keyRotation", "security");
            // Cek apakah job-nya memang terdaftar di scheduler
            if (!scheduler.checkExists(jobKey)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("Job detail tidak ditemukan di Quartz Scheduler", null));
            }

            // Pemicu manual (asynchronous, langsung return tanpa nunggu job selesai)
            scheduler.triggerJob(jobKey);
            return ResponseEntity.ok(new ApiResponse<>("Key rotation triggered", null));
        } catch (SchedulerException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(e.getMessage(), "failed to rotate keys"));
        }
    }
}

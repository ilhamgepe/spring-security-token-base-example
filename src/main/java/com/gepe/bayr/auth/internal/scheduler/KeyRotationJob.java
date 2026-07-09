package com.gepe.bayr.auth.internal.scheduler;


import com.gepe.bayr.auth.internal.service.RsaKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * Quartz job that rotates RSA signing keys on a schedule (default: every 30d).
 *
 * <p>Also purges fully-expired keys (expiresAt < now) to keep the table lean.
 * Keys are NOT deleted immediately on rotation – they're kept until {@code expires_at}
 * so ongoing JWT verifications don't fail.
 *
 * <p>Register this job in your Quartz config:
 * <pre>
 *   JobDetail job = JobBuilder.newJob(KeyRotationJob.class)
 *       .withIdentity("keyRotation", "security")
 *       .storeDurably()
 *       .build();
 *
 *   Trigger trigger = TriggerBuilder.newTrigger()
 *       .forJob(job)
 *       .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?")) // 02:00 daily
 *       .build();
 * </pre>
 */
@Slf4j
@Component
@DisallowConcurrentExecution // never run two rotations in parallel
@RequiredArgsConstructor
public class KeyRotationJob implements Job {
    private final RsaKeyService rsaKeyService;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("KeyRotationJob: starting RSA key rotation...");
        try {
            rsaKeyService.rotateKey();
        } catch (Exception e) {
            log.error("KeyRotationJob: FAILED – {}", e.getMessage(), e);

            throw new JobExecutionException(e);
        }
    }
}
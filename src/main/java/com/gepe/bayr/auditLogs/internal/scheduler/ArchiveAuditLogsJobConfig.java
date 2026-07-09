package com.gepe.bayr.auditLogs.internal.scheduler;


import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArchiveAuditLogsJobConfig {
    @Bean
    public JobDetail archiveAuditLogsJobDetail() {
        return JobBuilder.newJob(ArchiveAuditLogsJob.class)
                .withIdentity("archiveAuditLogsJob", "auditLogs")
                .withDescription("Archive old audit logs")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger archiveAuditLogsTrigger(@Qualifier("archiveAuditLogsJobDetail") JobDetail job) {
        return TriggerBuilder.newTrigger()
                .forJob(job)
                .withIdentity("archiveAuditLogsTrigger", "auditLogs")
                .withDescription("Archive old audit logs")
                .withSchedule(
                        CronScheduleBuilder
                                .cronSchedule("0 0 3 1 * ?") // setiap tanggal 1 jam 3 pagi
                )
                .build();
    }
}

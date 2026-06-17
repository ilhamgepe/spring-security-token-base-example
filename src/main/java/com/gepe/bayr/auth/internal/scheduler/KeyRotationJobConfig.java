package com.gepe.bayr.auth.internal.scheduler;


import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeyRotationJobConfig {
    @Bean
    public JobDetail keyRotationJobDetail(){
        return JobBuilder.newJob(KeyRotationJob.class)
                .withIdentity("keyRotation","security")
                .withDescription("Rotates RSA signing keys and purges expired ones")
                .storeDurably() // keep job even when no trigger is associated
                .build();
    }

    @Bean
    public Trigger KeyRotationTrigger(@Qualifier("keyRotationJobDetail") JobDetail keyRotationJobDetail){
        return TriggerBuilder.newTrigger()
                .forJob(keyRotationJobDetail)
                .withIdentity("keyRotationTrigger","security")
                .withDescription("every 1st of the month. 2 AM")
                .withSchedule(
                        CronScheduleBuilder
                                .cronSchedule("0 0 2 1 * ?") // Setiap tanggal 1, jam 02:00
                                .withMisfireHandlingInstructionFireAndProceed() // Kalau pas mati, jalankan langsung begitu app nyala
                ).build();

    }
}

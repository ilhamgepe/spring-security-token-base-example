package com.gepe.bayr.auth.internal.config;


import com.gepe.bayr.auth.internal.service.RsaKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs after Spring context is fully initialized.
 * Ensures an active RSA signing key exists before the application starts serving requests.
 *
 * <p>This is the "first boot" guard – if the DB is empty (fresh deploy), a key is generated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityBootstrap implements ApplicationRunner {
    private final RsaKeyService rsaKeyService;


    @Override
    public void run(@NonNull ApplicationArguments args) throws Exception {
        log.info("SecurityBootstrap: checking for active signing key...");
        rsaKeyService.ensureActiveKeyExists();
        log.info("SecurityBootstrap: signing key ready.");
    }
}

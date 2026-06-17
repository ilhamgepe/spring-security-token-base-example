package com.gepe.bayr.auth.internal.delivery.http;

import com.gepe.bayr.auth.internal.crypto.RsaKeyService;
import com.gepe.bayr.shared.web.response.ApiResponse;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class JwksController {
    private final RsaKeyService rsaKeyService;
    private final Scheduler scheduler;

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String jwks() {
        List<RSAKey> publicKeys = rsaKeyService.loadPublicKeysForJwks();
        JWKSet jwkSet = new JWKSet(publicKeys.stream().map(k -> (JWK) k).toList());
        return jwkSet.toPublicJWKSet().toString();
    }


    @PostMapping("/keys/rotate")
    public ResponseEntity<ApiResponse<String>> forceKeyRotation() {
        try{
            JobKey jobKey = new JobKey("keyRotation","security");
            // Cek apakah job-nya memang terdaftar di scheduler
            if (!scheduler.checkExists(jobKey)) {
                return ResponseEntity.status(404)
                        .body(new ApiResponse<>("Job detail tidak ditemukan di Quartz Scheduler", null));
            }

            // Pemicu manual (asynchronous, langsung return tanpa nunggu job selesai)
            scheduler.triggerJob(jobKey);
            return ResponseEntity.ok(new ApiResponse<>("Key rotation triggered", null));
        }catch (SchedulerException e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(e.getMessage(), "failed to rotate keys"));
        }
    }
}

package com.gepe.bayr.auth.internal.service;


import com.gepe.bayr.auth.exception.AuthError;
import com.gepe.bayr.auth.internal.entity.SigningKey;
import com.gepe.bayr.auth.internal.repo.SigningKeyRepo;
import com.gepe.bayr.shared.constants.CacheNames;
import com.gepe.bayr.shared.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SigningKeyCacheService {
    private final SigningKeyRepo signingKeyRepo;


    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.SIGNING_KEYS, key = "#kid")
    public SigningKey getSigningKey(String kid) {
        System.out.println("==============================");
        System.out.println("Cache miss, kid=" + kid);
        System.out.println("==============================");

        // cache miss
        return signingKeyRepo.findByKid(kid)
                .orElseThrow(() -> {
                    log.error("Unknown signing key kid={}", kid);
                    return new ServiceException(AuthError.AUTH_UNAUTHORIZED);
                });
    }

    @CacheEvict(cacheNames = CacheNames.SIGNING_KEYS, allEntries = true)
    public void evictAllSigningKeyCache() {
        log.info("Evict all signing key cache");
    }
}

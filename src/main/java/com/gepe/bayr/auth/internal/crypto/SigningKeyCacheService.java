package com.gepe.bayr.auth.internal.crypto;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.gepe.bayr.auth.internal.entity.SigningKey;
import com.gepe.bayr.auth.internal.repo.SigningKeyRepo;
import com.gepe.bayr.shared.exception.ErrorMessage;
import com.gepe.bayr.shared.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class SigningKeyCacheService {
    private final SigningKeyRepo signingKeyRepo;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_SIGNING_KEY = "signing-key:";
    private static final Duration REDIS_SIGNING_KEY_TTL = Duration.ofDays(10);


    @Transactional(readOnly = true)
    public SigningKey getSigningKey(String kid) {
        String redisKey = REDIS_KEY_SIGNING_KEY + kid;
        try{
            String cachedJson = redis.opsForValue().get(redisKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, SigningKey.class);
            }
            System.out.println("==============================");
            System.out.println("Cache miss, kid=" + kid);
            System.out.println("==============================");

            // cache miss
            SigningKey signingKey = signingKeyRepo.findByKid(kid)
                    .orElseThrow(() -> {
                        log.error("Unknown signing key kid={}" , kid);
                        return new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);
                    });

            try {
                redis.opsForValue().set(redisKey, objectMapper.writeValueAsString(signingKey), REDIS_SIGNING_KEY_TTL);

            }catch (Exception e){
                log.error("Error while saving signing key to redis, error={}", e.getMessage());
            }

            return signingKey;
        } catch (ServiceException e) {
            throw e; // Biarkan ServiceException lolos ke filter
        } catch (Exception e) {
            log.error("Failed to fetch signing key cache, error={}", e.getMessage());
            throw new ServiceException(ErrorMessage.SYSTEM_ERROR);
        }
    }

    public void evictAllSigningKeyCache() {
        Set<String> keys = redis.keys(REDIS_KEY_SIGNING_KEY+"*");
        if(!keys.isEmpty()){
            redis.delete(keys);
            log.info("Evict all signing key cache");
        }
    }
}

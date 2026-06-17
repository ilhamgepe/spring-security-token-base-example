package com.gepe.bayr.auth.internal.service;

import com.gepe.bayr.shared.exception.ErrorMessage;
import com.gepe.bayr.shared.exception.ServiceException;
import com.gepe.bayr.user.api.dto.UserRes;
import com.gepe.bayr.user.api.UserFacade;
import com.gepe.bayr.user.api.type.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Manages user authorities (roles + permissions) in Redis.
 *
 * <p><b>Why Redis and not JWT claims?</b>
 * Embedding all permissions in the JWT would bloat the token on every request.
 * Instead, on first access after login we fetch from DB and cache in Redis with a TTL
 * matching the AT lifetime. This keeps JWT slim while supporting instant revocation
 * by just evicting the Redis key.
 *
 * <p><b>Redis key format:</b>
 * <pre>
 *   auth:authorities:{userId}  →  Set of strings like ["ROLE_ADMIN", "wallet:read", ...]
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorityCacheService {
    private final StringRedisTemplate redis;
    private final UserFacade userFacade;

    private static final String SESSION_PREFIX = "auth:session:";
    private final ObjectMapper objectMapper;
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    public Collection<GrantedAuthority> getAuthorities(UUID userId){
        String redisKey = SESSION_PREFIX + userId.toString();
        try{
            String cachedJson = redis.opsForValue().get(redisKey);
            if(cachedJson != null){
                UserSessionCache userSessionCache = objectMapper.readValue(cachedJson, UserSessionCache.class);

                checkUserStatus(userSessionCache);

                return toGrantedAuthorities(userSessionCache.roles());
            }
        } catch (ServiceException e) {
            throw e; // Biarkan ServiceException lolos ke filter
        } catch (Exception e) {
            log.error("Failed to fetch session cache for userId={}", userId, e);
        }

        //CACHE MISS -> Ambil dari database lewat UserService
        UserRes userRes = userFacade.getByIdDetails(userId);

        // nanti kalo ada permission ubah ini,
        // buat query langsung aja buat get semua role dan permission jadi 1 set/list jgn pake getByIdDetails
        Set<String> roles = userRes.roles().stream()
                .map(role -> "ROLE_"+ role.code().toUpperCase())
                .collect(Collectors.toSet());

        UserSessionCache newSession = new UserSessionCache(
                userRes.status().name(),
                userRes.kycStatus().name(),
                roles
        );

        try {
            String jsonStr = objectMapper.writeValueAsString(newSession);
            redis.opsForValue().set(redisKey, jsonStr, CACHE_TTL);
        } catch (Exception e) {
            log.error("Gagal menyimpan cache Redis untuk userId={}", userId, e);
        }

        checkUserStatus(newSession);
        return toGrantedAuthorities(roles);
    }

    private void checkUserStatus(UserSessionCache session) {
        // Cek Status Akun
        if (UserStatus.SUSPENDED.name().equals(session.status())) {
            throw new ServiceException(ErrorMessage.USER_SUSPENDED);
        } else if (UserStatus.DISABLED.name().equals(session.status())) {
            throw new ServiceException(ErrorMessage.USER_DISABLED);
        }

        // Cek Status KYC (Contoh jika nanti kamu butuh validasi di level filter)
        // if ("BANNED_KYC".equals(session.kycStatus())) { ... }
    }


    private Collection<GrantedAuthority> toGrantedAuthorities(Set<String> raw) {
        return raw.stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }


    /**
     * Evicts the cached session for a user.
     * Cukup panggil 1 key ini, otomatis roles, status, dan kyc bersih sekaligus!
     */
    public void evict(UUID userId) {
        try {
            redis.delete(SESSION_PREFIX + userId.toString());
            log.debug("Evicted session cache for userId={}", userId);
        } catch (Exception ex) {
            log.error("Redis down while evict userId={}", userId);
        }
    }

    public void evictAll() {
        Set<String> keys = redis.keys(SESSION_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
            log.info("Evicted all {} session cache entries", keys.size());
        }
    }

    public record UserSessionCache(
            String status,      // ACTIVE, SUSPENDED, DISABLED
            String kycStatus,   // UNVERIFIED, PENDING, APPROVED, REJECTED
            Set<String> roles   // ["ROLE_ADMIN", "ROLE_USER", "wallet:read", "wallet:write"]
    ) {}
}

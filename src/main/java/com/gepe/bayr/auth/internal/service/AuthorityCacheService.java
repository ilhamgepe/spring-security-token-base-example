package com.gepe.bayr.auth.internal.service;

import com.gepe.bayr.auth.exception.AuthError;
import com.gepe.bayr.shared.constants.CacheNames;
import com.gepe.bayr.shared.exception.ServiceException;
import com.gepe.bayr.user.api.UserService;
import com.gepe.bayr.user.api.dto.UserDto;
import com.gepe.bayr.user.api.type.UserStatusType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

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
    private final CacheManager cacheManager;
    private final UserService userService;

    public Collection<GrantedAuthority> getAuthorities(UUID userId) {
        Cache cache = cacheManager.getCache(CacheNames.AUTH_SESSIONS);
        String key = userId.toString();

        // 1. Coba ambil data session dari cache
        if (cache != null) {
            UserSessionCache cachedSession = cache.get(key, UserSessionCache.class);
            if (cachedSession != null) {
                // Pengecekan status TETAP jalan walaupun cache HIT (Sangat Aman!)
                checkUserStatus(cachedSession);
                return toGrantedAuthorities(cachedSession.roles());
            }
        }

        // 2. CACHE MISS -> Ambil dari database lewat UserService
        log.info("Cache miss for user session, userId={}", userId);
        UserDto userDto = userService.getByIdDetails(userId);

        Set<String> roles = userDto.roles().stream()
                .map(role -> "ROLE_" + role.code().toUpperCase())
                .collect(Collectors.toSet());

        UserSessionCache newSession = new UserSessionCache(
                userDto.status().name(),
                userDto.kycStatus().name(),
                roles
        );

        // 3. Simpan ke cache secara otomatis (Jackson di-handle Spring Cache lewat default typing)
        if (cache != null) {
            cache.put(key, newSession);
        }

        checkUserStatus(newSession);
        return toGrantedAuthorities(roles);
    }

    private void checkUserStatus(UserSessionCache session) {
        if (UserStatusType.SUSPENDED.name().equals(session.status())) {
            throw new ServiceException(AuthError.USER_SUSPENDED);
        } else if (UserStatusType.DISABLED.name().equals(session.status())) {
            throw new ServiceException(AuthError.USER_DISABLED);
        }
    }

    private Collection<GrantedAuthority> toGrantedAuthorities(Set<String> raw) {
        return raw.stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }

    /**
     * Evict cache user tertentu (Misal saat logout atau status diubah)
     */
    public void evict(UUID userId) {
        Cache cache = cacheManager.getCache(CacheNames.AUTH_SESSIONS);
        if (cache != null) {
            cache.evict(userId.toString());
            log.debug("Evicted session cache for userId={}", userId);
        }
    }

    /**
     * Evict semua cache session
     */
    public void evictAll() {
        Cache cache = cacheManager.getCache(CacheNames.AUTH_SESSIONS);
        if (cache != null) {
            cache.clear(); // Menggantikan redis.delete(keys) massal dengan aman
            log.info("Evicted all session cache entries");
        }
    }

    public record UserSessionCache(
            String status,
            String kycStatus,
            Set<String> roles
    ) {
    }
}

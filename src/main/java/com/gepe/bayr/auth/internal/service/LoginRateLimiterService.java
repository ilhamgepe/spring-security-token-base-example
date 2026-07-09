package com.gepe.bayr.auth.internal.service;

import com.gepe.bayr.shared.exception.GlobalError;
import com.gepe.bayr.shared.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;

/**
 * Rate limiter login berbasis Redis dengan dua key terpisah per-username:
 *
 *  rate:login:user:fails:{username}   → counter kegagalan kumulatif (TTL 24 jam, TIDAK dihapus saat lockout habis)
 *  rate:login:user:lockout:{username} → flag lockout aktif (TTL = durasi lockout saat ini)
 *
 * Pemisahan ini memungkinkan exponential backoff yang benar:
 *   - Fails ke-5  → lockout 60s
 *   - Fails ke-6  → lockout 120s  (setelah lockout sebelumnya habis, counter tetap 6)
 *   - Fails ke-7  → lockout 240s
 *   - dst, cap 1 jam
 *
 * Per-IP: fixed window 10 request / 60 detik.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRateLimiterService {

    private final StringRedisTemplate redis;

    private static final String IP_KEY_PREFIX           = "rate:login:ip:";
    private static final String USER_FAILS_KEY_PREFIX   = "rate:login:user:fails:"; // counter kegagalan kumulatif (TTL 1 jam, TIDAK dihapus saat lockout habis)
    private static final String USER_LOCKOUT_KEY_PREFIX = "rate:login:user:lockout:"; // flag lockout aktif (TTL = durasi lockout saat ini)

    private static final int  IP_MAX_ATTEMPTS      = 10;
    private static final long IP_WINDOW_SECONDS    = 60L;

    private static final int  USER_MAX_FAILS        = 5;
    private static final long LOCKOUT_BASE_SECONDS  = 60L;
    private static final long LOCKOUT_MAX_SECONDS   = 3600L; // cap 1 jam
    private static final long FAILS_TTL_SECONDS     = 3600L; // counter hidup 1 jam sejak terakhir gagal

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public void checkIpAllowed(InetAddress ip) {
        String key = IP_KEY_PREFIX + ip.getHostAddress();
        try {
            String raw = redis.opsForValue().get(key);
            if (raw != null && Long.parseLong(raw) >= IP_MAX_ATTEMPTS) {
//                Long ttl = redis.getExpire(key);
//                long retryAfter = (ttl != null && ttl > 0) ? ttl : IP_WINDOW_SECONDS;
                throw new ServiceException(GlobalError.HTTP_TOO_MANY_ATTEMPTS);
            }
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(IP_WINDOW_SECONDS));
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("checkIpAllowed error ip={}: {}", ip.getHostAddress(), e.getMessage());
        }
    }

    public void checkUsernameAllowed(String username) {
        String lockoutKey = USER_LOCKOUT_KEY_PREFIX + normalize(username);
        try {
            // Cek apakah sedang dalam masa lockout
            Long ttl = redis.getExpire(lockoutKey);
            if (ttl != null && ttl > 0) {
                log.warn("Username locked username={}, retryAfter={}s", username, ttl);
                throw new ServiceException(GlobalError.AUTH_TOO_MANY_ATTEMPTS_WITH_DURATION, ttl);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("checkUsernameAllowed error username={}: {}", username, e.getMessage());
        }
    }

    public void onLoginFailed(String username) {
        String failsKey   = USER_FAILS_KEY_PREFIX + normalize(username);
        String lockoutKey = USER_LOCKOUT_KEY_PREFIX + normalize(username);
        try {
            // Increment counter kumulatif, perbarui TTL-nya supaya tidak expired saat user masih aktif
            Long fails = redis.opsForValue().increment(failsKey);
            if (fails == null) return;
            redis.expire(failsKey, Duration.ofSeconds(FAILS_TTL_SECONDS));

            if (fails >= USER_MAX_FAILS) {
                // Hitung durasi lockout berdasarkan total fails (exponential dari fails ke-5 dst)
                long exponent = fails - USER_MAX_FAILS; // 0, 1, 2, 3, ...
                long lockout  = Math.min(
                        LOCKOUT_BASE_SECONDS * (1L << Math.min(exponent, 10)),
                        LOCKOUT_MAX_SECONDS
                );
                // Set flag lockout terpisah — key ini yang expire, bukan counter
                redis.opsForValue().set(lockoutKey, "1", Duration.ofSeconds(lockout));
                log.warn("Username locked username={}, fails={}, lockout={}s", username, fails, lockout);
            }
        } catch (Exception e) {
            log.error("onLoginFailed error username={}: {}", username, e.getMessage());
        }
    }

    public void onLoginSuccess(String username) {
        try {
            String normalized = normalize(username);
            redis.delete(USER_FAILS_KEY_PREFIX + normalized);
            redis.delete(USER_LOCKOUT_KEY_PREFIX + normalized);
        } catch (Exception e) {
            log.error("onLoginSuccess reset error username={}: {}", username, e.getMessage());
        }
    }

    // ------------------------------------------------------------------

    private String normalize(String username) {
        return username.toLowerCase().trim();
    }
}
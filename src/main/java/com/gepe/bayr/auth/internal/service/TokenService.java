package com.gepe.bayr.auth.internal.service;

import com.gepe.bayr.auth.api.event.ReplayAttackDetectedEvent;
import com.gepe.bayr.auth.internal.crypto.RsaKeyService;
import com.gepe.bayr.auth.internal.crypto.SigningKeyCacheService;
import com.gepe.bayr.auth.internal.entity.RefreshSession;
import com.gepe.bayr.auth.internal.entity.SigningKey;
import com.gepe.bayr.auth.internal.repo.RefreshSessionRepo;
import com.gepe.bayr.auth.api.event.SecurityEventListener;
import com.gepe.bayr.shared.exception.ErrorMessage;
import com.gepe.bayr.shared.exception.ServiceException;
import com.github.f4b6a3.uuid.UuidCreator;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenService {
    private final RsaKeyService rsaKeyService;
    private final RefreshSessionRepo refreshSessionRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final SigningKeyCacheService signingKeyCacheService;

    private static final SecureRandom secureRandom =  new SecureRandom();



    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.access-token-ttl:15m}")   // 15 min
    private Duration accessTokenTtl;

    @Value("${app.jwt.refresh-token-ttl:15d}") // 15 days
    private Duration refreshTokenTtl;


    /**
     * Ngasih AT & RT baru dan simpen refresh token dan sessionnya ke db
     */
    @Transactional
    public TokenPair issueTokenPair(UUID userId, String userAgent, InetAddress ipAddress) {
        RSAKey activeKey = rsaKeyService.loadActiveRsaKey();
        SigningKey keyEntity = signingKeyCacheService.getSigningKey(activeKey.getKeyID());


        String AT = buildAccessToken(userId,activeKey);

        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        String rawOpaque = generateRefreshToken(); // Mentah (Secret)

        String RT = sessionId.toString() + "." + rawOpaque; // ini pake raw, yang di db baru di hashed ya

        String hashedOpaque = hashOpaque(rawOpaque);

        RefreshSession session = new RefreshSession();
        session.setUserId(userId);
        session.setSessionId(sessionId);
        session.setRefreshTokenHash(hashedOpaque);
        session.setSigningKey(keyEntity);
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setIsRevoked(false);
        session.setExpiresAt(Instant.now().plus(refreshTokenTtl));
        session.setCreatedAt(Instant.now());

        refreshSessionRepo.save(session);
        return new TokenPair(AT, RT);
    }

    /**
     * Rotate refresh token
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenPair rotateRefreshToken(String rawRefreshToken, String userAgent, InetAddress ipAddress) {
        RefreshTokenParts parts = extractRefreshTokenParts(rawRefreshToken);

        RefreshSession session = refreshSessionRepo.findBySessionId(parts.sessionId())
                .orElseThrow(() -> new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED));

        if (!validateRawRefreshToken(parts.rawOpaque, session.getRefreshTokenHash())) {
            throw new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);
        }

        if (session.getIsRevoked()) {
            log.warn("REPLAY ATTACK detected for userId={}, revoking all sessions", session.getUserId());
            eventPublisher.publishEvent(new ReplayAttackDetectedEvent(session.getUserId()));
            throw new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);
        }

        // Revoke old session
        session.setIsRevoked(true);
        session.setRevokedAt(Instant.now());
        session.setRevokeReason("ROTATED_REFRESH_TOKEN");
        refreshSessionRepo.save(session);

        return issueTokenPair(session.getUserId(), userAgent, ipAddress);
    }

    /**
     * revoke session pas logout
     */
    @Transactional
    public void revokeSession(UUID userId, String rawRefreshToken) {
        RefreshTokenParts parts = extractRefreshTokenParts(rawRefreshToken);
        RefreshSession session = refreshSessionRepo.findBySessionId(parts.sessionId())
                .orElseThrow(() -> new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED));

        if (!validateRawRefreshToken(parts.rawOpaque, session.getRefreshTokenHash())) {
            throw new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);
        }

        session.setIsRevoked(true);
        session.setRevokedAt(Instant.now());
        session.setRevokeReason("LOGOUT");
        refreshSessionRepo.save(session);
    }


    public JWTClaimsSet verifyAccessToken(String rawToken) {
        try{
            SignedJWT jwt = SignedJWT.parse(rawToken);
            String kid = jwt.getHeader().getKeyID();

            SigningKey keyEntity = signingKeyCacheService.getSigningKey(kid);

            RSAKey publicKey = rsaKeyService.parsePublicRsaKeyOnly(kid, keyEntity.getPublicKey());
            JWSVerifier verifier = new RSASSAVerifier(publicKey);

            if(!jwt.verify(verifier)){
                throw new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            if (claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);
            }

            return claims;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Malformed access token: {}",  e.getMessage());
            throw new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);
        }
    }


    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildAccessToken(UUID userId, RSAKey signingKey) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(userId.toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(accessTokenTtl)))
                    // Deliberately minimal – no roles/permissions in claims
                    // Authorities are loaded from Redis on each request
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(signingKey.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build access token", e);
        }
    }
    public record TokenPair(String accessToken, String refreshToken) {}


    private String generateRefreshToken() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashOpaque(String rawOpaque) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawOpaque.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to get MessageDigest instance", e);
        }
    }

    private record RefreshTokenParts(UUID sessionId, String rawOpaque) {}
    private RefreshTokenParts extractRefreshTokenParts(String rawRefreshToken) {
        String[] parts = rawRefreshToken.split("\\.");
        if (parts.length != 2) throw new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);

        String sessionIdStr = parts[0];
        String rawOpaque = parts[1];

        UUID sessionId;
        try {
            sessionId = UUID.fromString(sessionIdStr);
        } catch (IllegalArgumentException e) {
            // malform session id (uuid)
            throw new ServiceException(ErrorMessage.AUTH_UNAUTHORIZED);
        }
        return new RefreshTokenParts(sessionId, rawOpaque);
    }

    /**
     * Memvalidasi token mentah dari user dengan cara di-hash ulang
     * lalu dicocokkan dengan hash yang ada di DB.
     */
    private Boolean validateRawRefreshToken(String rawOpaqueFromUser, String hashedOpaqueFromDb) {
        if (rawOpaqueFromUser == null || hashedOpaqueFromDb == null) return false;

        // Hash token mentah bawaan user
        String hashedUserOpaque = hashOpaque(rawOpaqueFromUser);

        // Bandingkan dengan hash di DB
        // MessageDigest.isEqual (Java 6+) sudah dirancang resisten timing-attack.
        return MessageDigest.isEqual(
                hashedOpaqueFromDb.getBytes(StandardCharsets.UTF_8),
                hashedUserOpaque.getBytes(StandardCharsets.UTF_8)
        );
    }



}

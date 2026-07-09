package com.gepe.bayr.auth.internal.service;


import com.gepe.bayr.auth.internal.crypto.PrivateKeyEncryptor;
import com.gepe.bayr.auth.internal.entity.SigningKey;
import com.gepe.bayr.auth.internal.repo.SigningKeyRepo;
import com.gepe.bayr.shared.constants.CacheNames;
import com.github.f4b6a3.uuid.UuidCreator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RSA Key Service
 * manage rsa key pair
 * <Br/>1. generate 2048 bit rsa key pair pake nimbus jose
 * <Br/>2. encrypt private key pake Tink AES256 GCM sebelom simpan ke databse
 * <Br/>3. decrypt private key pake Tink AES256 GCM
 * <Br/>4. tandain private key ayng active sebelumnya jadi inactive (graceful rotation)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RsaKeyService {

    private final SigningKeyRepo signingKeyRepo;
    private final PrivateKeyEncryptor privateKeyEncryptor;
    private final SigningKeyCacheService signingKeyCacheService;

    private static final int RSA_KEY_SIZE = 2048;

    @Value("${app.rsa.rotation-ttl}")
    private Duration RSA_ROTATION_TTL;

    @Transactional(rollbackFor = Exception.class)
    public void ensureActiveKeyExists() {
        if(signingKeyRepo.countByIsActiveTrue() == 0){
            log.info("No active signing key found – generating initial RSA key pair.");
            rotateKey();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.JWKS, allEntries = true)
    public void rotateKey(){
        RSAKey rsaKey = generateRsaKeyPair();

        String publicPem = generatePublicPem(rsaKey);
        String privatePem = generatePrivatePem(rsaKey);
        String encryptedPrivate = privateKeyEncryptor.encrypt(privatePem);

        // deactive key rsa yang lama. nanti di ganti yang baru
        signingKeyRepo.deactivateAllActiveKeys();
        signingKeyCacheService.evictAllSigningKeyCache();

        SigningKey newKey = new SigningKey();

        // Set nilainya satu per satu
        newKey.setKid(rsaKey.getKeyID());
        newKey.setAlgorithm("RS256");
        newKey.setPublicKey(publicPem);
        newKey.setPrivateKeyEnc(encryptedPrivate);
        newKey.setIsActive(true);
        newKey.setCreatedAt(Instant.now());
        newKey.setExpiresAt(Instant.now().plus(RSA_ROTATION_TTL));

        SigningKey saved = signingKeyRepo.save(newKey);

        log.info("Rotated signing key – new kid={}, expires={}", saved.getKid(), saved.getExpiresAt());

        // delete expired keys
        int deleted = signingKeyRepo.deleteExpiredKeys(Instant.now());
        log.info("KeyRotationJob: rotation complete. Purged {} expired key(s).", deleted);
    }

    @Transactional(readOnly = true)
    public RSAKey loadActiveRsaKey(){
        SigningKey activeKey = signingKeyRepo.findTopByIsActiveTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("No active signing key – this should never happen! whyyyyy"));

        String privatePem = privateKeyEncryptor.decrypt(activeKey.getPrivateKeyEnc());

        return parseRsaKey(activeKey.getKid(), activeKey.getPublicKey(), privatePem);
    }


    @Cacheable(cacheNames = CacheNames.JWKS)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> loadPublicKeysForJwks(){
        return signingKeyRepo.findAllNotExpiredOrActive(Instant.now()).stream()
                .map(k -> new java.util.LinkedHashMap<String, Object>(
                        parsePublicRsaKeyOnly(k.getKid(), k.getPublicKey()).toJSONObject()
                ))
                .collect(Collectors.toList());
    }


    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------
    private RSAKey generateRsaKeyPair() {
        try {
            return new RSAKeyGenerator(RSA_KEY_SIZE)
                    .keyID(UuidCreator.getTimeOrderedEpoch().toString())
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("RSA key generation failed", e);
        }
    }

    private String generatePublicPem(RSAKey rsaKey) {
        try {
            return "-----BEGIN PUBLIC KEY-----\n"
                    + java.util.Base64.getMimeEncoder(64, new byte[]{'\n'})
                    .encodeToString(rsaKey.toPublicKey().getEncoded())
                    + "\n-----END PUBLIC KEY-----";
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract public PEM", e);
        }
    }

    private String generatePrivatePem(RSAKey rsaKey) {
        try {
            return "-----BEGIN PRIVATE KEY-----\n"
                    + java.util.Base64.getMimeEncoder(64, new byte[]{'\n'})
                    .encodeToString(rsaKey.toPrivateKey().getEncoded())
                    + "\n-----END PRIVATE KEY-----";
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract private PEM", e);
        }
    }

    private byte[] removePemFormat(String pem) {
        String stripped = pem
                .replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");
        return java.util.Base64.getDecoder().decode(stripped);
    }

    public RSAKey parseRsaKey(String kid, String publicPem, String privatePem){
        try{
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] pubBytes  = removePemFormat(publicPem);
            byte[] privBytes = removePemFormat(privatePem);

            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(pubBytes));
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(kid).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA key pair from PEM", e);
        }
    }

    public RSAKey parsePublicRsaKeyOnly(String kid, String publicPem){
        try{
          KeyFactory keyFactory = KeyFactory.getInstance("RSA");
          byte[] pubBytes = removePemFormat(publicPem);

          RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(pubBytes));
            return new RSAKey.Builder(publicKey).keyID(kid).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key from PEM", e);
        }
    }
}

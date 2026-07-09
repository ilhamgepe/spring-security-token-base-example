package com.gepe.bayr.auth.internal.crypto;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Slf4j
@Component
public class PrivateKeyEncryptor {
    private final Aead aead;

    // ini Authenticated Encryption with Associated Data (aead)
    // ini kaya key tp tidak ikut di encrypt, tapi tetep di verify
    // jadi jika pas di decrypt semuanya benar tapi associated data tidak benar/berbeda, maka tidak valid
    // ini juga jadi bisa membedakan, misal mau encrypt ktp, maka associated data di kasih nama aja ktp_assoc atau apa gitu.
    // jadi tidak perlu keysetjson berbeda untuk semua associated data
    private static final byte[] ASSOCIATED_DATA = "signing_key_private".getBytes(StandardCharsets.UTF_8);

    public PrivateKeyEncryptor(@Value("${app.tink.keyset-json}") String keysetJson) {
        try {
            AeadConfig.register();
            KeysetHandle keysetHandle = TinkJsonProtoKeysetFormat.parseKeyset(keysetJson, InsecureSecretKeyAccess.get());
            this.aead = keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Tink AEAD – check app.tink.keyset-json", e);
        }
    }

    /**
     * encrypt private pem dan return cipher atau string yang sudah di encrypt
     * lalu di encode base 64
     */
    public String encrypt(String privatePem) {
        try {
            byte[] cipherText = aead.encrypt(
                    privatePem.getBytes(StandardCharsets.UTF_8),
                    ASSOCIATED_DATA
            );
            return Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt private key", e);
        }
    }

    /**
     * decode base64 string yang di encrypt tadi jadi String pem yang asli
     */
    public String decrypt(String base64Ciphertext) {
        try {
            if (base64Ciphertext == null) {
                throw new NullPointerException("Ciphertext cannot be null");
            }
            byte[] plainText = aead.decrypt(Base64.getDecoder().decode(base64Ciphertext), ASSOCIATED_DATA);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt private key – keyset mismatch or data corruption", e);
        }
    }


    /**
     * generate new keyset json, wajib di jalanin lewat unit test atau api,
     * lalu set di env agar app.properties app.tink.keyset-json tersedia
     */
    public static String generateNewKeysetJSON() {
        try {
            AeadConfig.register();
            KeysetHandle keysetHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM);
            return TinkJsonProtoKeysetFormat.serializeKeyset(keysetHandle, InsecureSecretKeyAccess.get());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate new Tink keyset", e);
        }
    }
}

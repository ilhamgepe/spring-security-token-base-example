package com.gepe.bayr.auth.internal.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrivateKeyEncryptorTest {
    private PrivateKeyEncryptor encryptor;

    @BeforeEach
    void setUp() {
        String validKeysetJson = PrivateKeyEncryptor.generateNewKeysetJSON();
        this.encryptor = new PrivateKeyEncryptor(validKeysetJson);
    }

    @Nested
    class EncryptAndDecryptWorkflow {
        @Test
        void shouldSuccessfullyEncryptAndDecryptValidPemKey() {
            // Arrange
            String originalPem = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----";

            // Act
            String ciphertext = encryptor.encrypt(originalPem);
            String decrypted = encryptor.decrypt(ciphertext);

            // Assert
            assertNotNull(ciphertext);
            assertNotEquals(originalPem, ciphertext, "Ciphertext should be obfuscated");
            assertEquals(originalPem, decrypted, "Decrypted text must match original input");
        }

        @Test
        void shouldHandleEmptyAndSpecialCharactersStrings() {
            // Arrange
            String specialInput = "special-@_#-$*%^&*()_+{}[]";

            // Act & Assert
            String ciphertext = encryptor.encrypt(specialInput);
            assertEquals(specialInput, encryptor.decrypt(ciphertext));
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldThrowExceptionWhenDecryptingTamperedOrInvalidData() {
            // 1. Invalid Base64 format -> Melempar IllegalArgumentException
            String invalidBase64 = "bukan-base64-sama-sekali!";
            assertThrows(IllegalArgumentException.class, () -> encryptor.decrypt(invalidBase64));

            // 2. Format Base64 valid tapi isinya diubah -> Melempar IllegalStateException (karena gagal di Tink AEAD)
            String tamperedCiphertext = "SGVsbG8gV29ybGQ=";
            assertThrows(IllegalStateException.class, () -> encryptor.decrypt(tamperedCiphertext));

            // 3. Null input -> Melempar NullPointerException (atau IllegalArgumentException tergantung penangananmu)
            assertThrows(NullPointerException.class, () -> encryptor.decrypt(null));
        }

        @Test
        void shouldThrowExceptionWhenConstructorReceivesInvalidJson() {
            // Arrange
            String brokenJson = "{ \"invalid\": \"json_format\" }";

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> new PrivateKeyEncryptor(brokenJson));
            assertThrows(IllegalStateException.class, () -> new PrivateKeyEncryptor(null));
        }
    }

    @Nested
    class KeyGeneratorTool {

        @Test
        void shouldGenerateValidJsonKeysetFormat() {
            // Act
            String keysetJson = PrivateKeyEncryptor.generateNewKeysetJSON();

            // Assert
            assertNotNull(keysetJson);

            // PERUBAHAN DI SINI: Tambahkan .trim() sebelum melakukan check
            String trimmedJson = keysetJson.trim();
            assertTrue(trimmedJson.startsWith("{") && trimmedJson.endsWith("}"));

            // Mencetak ke console untuk kebutuhan copy-paste token pertama kali
            System.out.println("\n=== COPY THIS FOR TINK_KEYSET_JSON ===");
            System.out.println(trimmedJson); // Gunakan yang sudah di-trim agar rapi di env var
            System.out.println("======================================\n");
        }
    }
}
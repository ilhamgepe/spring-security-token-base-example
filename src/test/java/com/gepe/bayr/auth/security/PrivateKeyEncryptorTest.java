package com.gepe.bayr.auth.security;

import com.gepe.bayr.auth.internal.crypto.PrivateKeyEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrivateKeyEncryptorTest {
    private PrivateKeyEncryptor encryptor;

    @BeforeEach
    void setUp(){
        String validKeysetJson = PrivateKeyEncryptor.generateNewKeysetJson();
        this.encryptor = new PrivateKeyEncryptor(validKeysetJson);
    }

    @Nested
    class EncryptAndDecryptWorkflow{
        @Test
        void shouldSuccessfullyEncryptAndDecryptValidPemKey(){
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
            // Arrange
            String invalidBase64 = "bukan-base64-sama-sekali!";
            String tamperedCiphertext = "SGVsbG8gV29ybGQ="; // Base64 dari "Hello World", bukan buatan Tink

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> encryptor.decrypt(invalidBase64));
            assertThrows(IllegalStateException.class, () -> encryptor.decrypt(tamperedCiphertext));
            assertThrows(IllegalStateException.class, () -> encryptor.decrypt(null));
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
            String keysetJson = PrivateKeyEncryptor.generateNewKeysetJson();

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
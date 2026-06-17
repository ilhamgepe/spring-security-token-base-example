package com.gepe.bayr.auth.internal.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "signing_keys")
public class SigningKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "kid",nullable = false)
    private String kid;

    @Column(name = "algorithm",nullable = false)
    private String algorithm;

    @Column(name = "public_key",nullable = false)
    private String publicKey;

    @Column(name = "private_key_enc", nullable = false)
    private String privateKeyEnc;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}

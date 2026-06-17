package com.gepe.bayr.auth.internal.repo;

import com.gepe.bayr.auth.api.type.AuthProviderType;
import com.gepe.bayr.auth.internal.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CredentialRepo extends JpaRepository<Credential, Long> {

    Optional<Credential> findByProviderAndProviderId(AuthProviderType provider, String providerId);
}

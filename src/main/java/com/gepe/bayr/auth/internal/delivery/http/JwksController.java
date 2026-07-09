package com.gepe.bayr.auth.internal.delivery.http;

import com.gepe.bayr.auth.internal.service.RsaKeyService;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {
    private final com.gepe.bayr.auth.internal.service.RsaKeyService rsaKeyService;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return Map.of("keys", rsaKeyService.loadPublicKeysForJwks());
    }


}

package com.gepe.bayr.auth.internal.service;

import com.gepe.bayr.auth.api.type.AuthProviderType;
import com.gepe.bayr.auth.internal.delivery.http.res.LoginRes;
import com.gepe.bayr.auth.internal.entity.Credential;
import com.gepe.bayr.auth.internal.repo.CredentialRepo;
import com.gepe.bayr.shared.exception.ErrorMessage;
import com.gepe.bayr.shared.exception.ServiceException;
import com.gepe.bayr.user.api.dto.UserRes;
import com.gepe.bayr.user.api.UserFacade;
import com.gepe.bayr.user.api.type.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@Service
public class LocalAuthService {
    private final UserFacade userFacade;
    private final CredentialRepo credentialRepo;
    private final TokenService tokenService;
    private final LoginRateLimiterService rateLimiterService;
    private final BCryptPasswordEncoder bcrypt;

    @Transactional(rollbackFor = Exception.class)
    public TokenService.TokenPair register(String username, String password,String nickname, String userAgent, InetAddress ipAddress){
        // create user and user profile
        UserRes userRes = userFacade.registerUser(username,nickname);

        Credential credential =  new Credential();
        credential.setUserId(userRes.id());
        credential.setProvider(AuthProviderType.LOCAL);
        credential.setProviderId(username);
        credential.setPasswordHash(bcrypt.encode(password));
        credentialRepo.save(credential);

        return tokenService.issueTokenPair(userRes.id(), userAgent, ipAddress);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginRes login(String username, String password, String userAgent, InetAddress ipAddress){
        // Cek rate limit (per-IP & per-username) SEBELUM apapun lainnya
        rateLimiterService.checkIpAllowed(ipAddress);
        rateLimiterService.checkUsernameAllowed(username);

        Credential credential =  credentialRepo.findByProviderAndProviderId(AuthProviderType.LOCAL, username)
                .orElseGet(() -> {
                    // User tidak ditemukan -> tetap hitung sebagai failed attempt
                    // (mencegah enumeration & tetap kena rate limit)
                    rateLimiterService.onLoginFailed(username);
                    throw new ServiceException(ErrorMessage.AUTH_INVALID_CREDENTIALS);
                });

        UserRes userRes = userFacade.getByIdDetails(credential.getUserId());
        if(!userRes.status().equals(UserStatus.ACTIVE)){
            if(userRes.status().equals(UserStatus.DISABLED)){
                throw new ServiceException(ErrorMessage.USER_DISABLED);
            } else if (userRes.status().equals(UserStatus.SUSPENDED)) {
                throw new ServiceException(ErrorMessage.USER_SUSPENDED);
            }else {
                throw new ServiceException(ErrorMessage.SYSTEM_ERROR);
            }
        }

        if(!bcrypt.matches(password,credential.getPasswordHash())){
            rateLimiterService.onLoginFailed(username);
            throw new ServiceException(ErrorMessage.AUTH_INVALID_CREDENTIALS);
        }

        rateLimiterService.onLoginSuccess(username);
        TokenService.TokenPair tokenPair = tokenService.issueTokenPair(userRes.id(), userAgent, ipAddress);
        return new LoginRes(tokenPair.accessToken(), tokenPair.refreshToken(), userRes);
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout(UUID userId, String refreshToken){
        tokenService.revokeSession(userId, refreshToken);
    }
}
package com.gepe.bayr.auth.internal.service;

import com.gepe.bayr.auditLogs.api.event.AuditLogEvent;
import com.gepe.bayr.auth.internal.delivery.http.res.LoginRes;
import com.gepe.bayr.auth.api.type.AuthProviderType;
import com.gepe.bayr.auth.exception.AuthError;
import com.gepe.bayr.auth.internal.entity.Credential;
import com.gepe.bayr.auth.internal.repo.CredentialRepo;
import com.gepe.bayr.shared.constants.SystemUsers;
import com.gepe.bayr.shared.exception.GlobalError;
import com.gepe.bayr.shared.exception.ServiceException;
import com.gepe.bayr.shared.web.context.RequestContext;
import com.gepe.bayr.user.api.UserService;
import com.gepe.bayr.user.api.dto.UserDto;
import com.gepe.bayr.user.api.type.UserStatusType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@Service
public class LocalAuthService {
    private final UserService userService;
    private final CredentialRepo credentialRepo;
    private final TokenService tokenService;
    private final LoginRateLimiterService rateLimiterService;
    private final BCryptPasswordEncoder bcrypt;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public TokenService.TokenPair register(String username, String password, String nickname, String userAgent, InetAddress ipAddress) {
        // create user and user profile
        UserDto userDto = userService.registerUser(username, nickname);

        Credential credential = new Credential();
        credential.setUserId(userDto.id());
        credential.setProvider(AuthProviderType.LOCAL);
        credential.setProviderId(username);
        credential.setPasswordHash(bcrypt.encode(password));
        credentialRepo.save(credential);

        eventPublisher.publishEvent(AuditLogEvent.builder()
                .actorUserId(SystemUsers.SYSTEM_USER_ID)
                .actorEmail(SystemUsers.SYSTEM_USER_EMAIL)
                .requestId(RequestContext.getRequestId())
                .entityType("User")
                .entityId(userDto.id().toString())
                .action("USER_REGISTERED")
                .oldData(null)
                .newData(Map.of("UserRes", userDto))
                .build());

        return tokenService.issueTokenPair(userDto.id(), userAgent, ipAddress);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginRes login(String username, String password, String userAgent, InetAddress ipAddress) {
        // Cek rate limit (per-IP & per-username) SEBELUM apapun lainnya
        rateLimiterService.checkIpAllowed(ipAddress);
        rateLimiterService.checkUsernameAllowed(username);

        Credential credential = credentialRepo.findByProviderAndProviderId(AuthProviderType.LOCAL, username)
                .orElseGet(() -> {
                    // User tidak ditemukan -> tetap hitung sebagai failed attempt
                    // (mencegah enumeration & tetap kena rate limit)
                    rateLimiterService.onLoginFailed(username);
                    throw new ServiceException(AuthError.AUTH_INVALID_CREDENTIALS);
                });

        UserDto userDto = userService.getByIdDetails(credential.getUserId());
        if (!userDto.status().equals(UserStatusType.ACTIVE)) {
            if (userDto.status().equals(UserStatusType.DISABLED)) {
                throw new ServiceException(AuthError.USER_DISABLED);
            } else if (userDto.status().equals(UserStatusType.SUSPENDED)) {
                throw new ServiceException(AuthError.USER_SUSPENDED);
            } else {
                throw new ServiceException(GlobalError.SYSTEM_ERROR);
            }
        }

        if (!bcrypt.matches(password, credential.getPasswordHash())) {
            rateLimiterService.onLoginFailed(username);
            throw new ServiceException(AuthError.AUTH_INVALID_CREDENTIALS);
        }

        rateLimiterService.onLoginSuccess(username);
        TokenService.TokenPair tokenPair = tokenService.issueTokenPair(userDto.id(), userAgent, ipAddress);
        return new LoginRes(tokenPair.accessToken(), tokenPair.refreshToken(), userDto);
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout(UUID userId, String refreshToken) {
        tokenService.revokeSession(userId, refreshToken);
    }
}
package com.gepe.bayr.auth.internal.delivery.http;


import com.gepe.bayr.auth.internal.delivery.http.req.LoginReq;
import com.gepe.bayr.auth.internal.delivery.http.req.RefreshReq;
import com.gepe.bayr.auth.internal.delivery.http.req.RegisterReq;
import com.gepe.bayr.auth.internal.delivery.http.res.LoginRes;
import com.gepe.bayr.auth.internal.service.LocalAuthService;
import com.gepe.bayr.auth.internal.service.TokenService;
import com.gepe.bayr.shared.config.i18n.MessageHelper;
import com.gepe.bayr.shared.web.response.ApiResponse;
import com.gepe.bayr.user.api.UserService;
import com.gepe.bayr.user.api.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/v1/auth")
public class AuthController {
    private final LocalAuthService localAuthService;
    private final UserService userService;
    private final MessageHelper messageHelper;
    private final TokenService tokenService;

    @PostMapping(value = "/register")
    public ResponseEntity<ApiResponse<TokenService.TokenPair>> register(@Valid @RequestBody RegisterReq req, HttpServletRequest httpReq) {
        TokenService.TokenPair tokenPair = localAuthService.register(req.email(), req.password(),
                req.nickname(), userAgent(httpReq), clientIp(httpReq));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(messageHelper.get("auth.register_success"), tokenPair));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginRes>> login(@Valid @RequestBody LoginReq req, HttpServletRequest httpReq) {
        LoginRes res = localAuthService.login(req.email(), req.password(), userAgent(httpReq), clientIp(httpReq));
        return ResponseEntity.ok(new ApiResponse<>(messageHelper.get("auth.login_success"), res));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(Authentication authentication, @Valid @RequestBody RefreshReq req) {
        UUID userId = (UUID) authentication.getPrincipal();
        localAuthService.logout(userId, req.refreshToken());
        return ResponseEntity.ok(new ApiResponse<>(messageHelper.get("auth.logout_success"), null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenService.TokenPair>> refresh(@Valid @RequestBody RefreshReq req, HttpServletRequest httpReq) {
        String refreshToken = req.refreshToken();
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(messageHelper.get("http.unauthorized"), null));
        }
        TokenService.TokenPair tokenPair = tokenService.rotateRefreshToken(refreshToken, userAgent(httpReq), clientIp(httpReq));
        return ResponseEntity.ok(new ApiResponse<>(messageHelper.get("auth.refresh_success"), tokenPair));
    }


    @GetMapping("/me")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ApiResponse<UserDto>> getMe(Authentication authentication) {
        UUID uuid = (UUID) authentication.getPrincipal();
        UserDto user = userService.getByIdDetails(uuid);
        return ResponseEntity.ok().body(new ApiResponse<>(messageHelper.get("common.success"), user));
    }


    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String userAgent(HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }

    private InetAddress clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        String ipStr = (forwarded != null) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();

        try {
            return InetAddress.getByName(ipStr);
        } catch (Exception e) {
            // Log error jika diperlukan, lalu return null atau loopback address sebagai fallback
            log.error("Error getting client IP address: {}", e.getMessage());
            return InetAddress.getLoopbackAddress();
        }
    }
}
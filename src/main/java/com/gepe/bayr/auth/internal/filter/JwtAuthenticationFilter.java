package com.gepe.bayr.auth.internal.filter;

import com.gepe.bayr.auth.internal.service.AuthorityCacheService;
import com.gepe.bayr.auth.internal.service.TokenService;
import com.gepe.bayr.shared.exception.ServiceException;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

/**
 * Extracts the Bearer token from Authorization header, verifies its RS256 signature,
 * then loads the user's authorities from Redis and sets the SecurityContext.
 *
 * <p>Authorities come from Redis (not JWT claims) – keeping the token slim and
 * allowing instant permission revocation without re-issuing tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final AuthorityCacheService authorityCacheService;

    private static final String BEARER_PREFIX = "Bearer ";


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String rawToken = header.substring(BEARER_PREFIX.length()).trim();

        try{
            JWTClaimsSet claims = tokenService.verifyAccessToken(rawToken);
            UUID userId = UUID.fromString(claims.getSubject());

            Collection<GrantedAuthority> authorities = authorityCacheService.getAuthorities(userId);

            // Create authenticated token – credentials=null because we're past verification
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId,null,authorities);

            // auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }catch (ServiceException e){
            log.debug("JWT authentication failed: {}", e.getMessage());
        }
        chain.doFilter(request, response);
    }
}

package com.gepe.bayr.auth.internal.config;

import com.gepe.bayr.auth.internal.filter.JwtAuthenticationFilter;
import com.gepe.bayr.shared.config.i18n.MessageHelper;
import com.gepe.bayr.shared.web.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize, @PostAuthorize, @Secured
@RequiredArgsConstructor
public class securityConfig {

    private final MessageHelper messageHelper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter, CorsConfigurationSource corsConfigurationSource, ObjectMapper objectMapper) throws Exception {
        return http
                // Stateless API – no sessions, no CSRF cookies
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> {
                    // Public auth endpoints
                    auth.requestMatchers(HttpMethod.POST, "/app/v1/auth/login").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/app/v1/auth/refresh").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/app/v1/auth/register").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/app/v1/auth/oauth2/**").permitAll();

                    // Public JWKS endpoint so resource servers can verify JWTs
                    auth.requestMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll();

                    // donations
                    auth.requestMatchers(HttpMethod.POST, "/app/v1/donations").permitAll();

                    auth.requestMatchers("/actuator/health").permitAll();
                    auth.requestMatchers("/actuator/**").hasRole("ADMIN");

                    // operation
//                    auth.requestMatchers("/operations/**").hasRole("ADMIN");

                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // 2. Gunakan ErrorResponse Record milikmu di sini
                .exceptionHandling(ex -> {
                    // Handle 401 Unauthorized (Bisa karena ga bawa token / token busuk)
                    ex.authenticationEntryPoint((request, response, e) -> {
                        // 2. Ambil pesan dinamis dari i18n menggunakan key di properties-mu
                        String localizedMessage = messageHelper.get("http.unauthorized");

                        ErrorResponse errorBody = new ErrorResponse(
                                localizedMessage,
                                null
                        );

                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");

                        String jsonResponse = objectMapper.writeValueAsString(errorBody);
                        response.getWriter().write(jsonResponse);
                    });

                    // Handle 403 Forbidden (Token valid, tapi role/permission tidak cukup)
                    ex.accessDeniedHandler((request, response, accessDeniedException) -> {

                        // 3. Ambil pesan dari i18n untuk forbidden
                        String localizedMessage = messageHelper.get("http.forbidden");

                        ErrorResponse errorBody = new ErrorResponse(
                                localizedMessage,
                                null
                        );

                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");

                        String jsonResponse = objectMapper.writeValueAsString(errorBody);
                        response.getWriter().write(jsonResponse);
                    });
                })
                .build();
    }


    @Bean
    public BCryptPasswordEncoder bcryptPasswordEncoder() {
        // Strength 12 is the current industry standard (≈250ms on modern hardware)
        return new BCryptPasswordEncoder(12);
    }

//    ini ga kepake sih karna kan nanti di frontend dia ada backend sendiri(bff) jd yang komunikasi itu bff ke spring, bukan browser ke spring jd ga perlu cors
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource(){
//        CorsConfiguration config = new CorsConfiguration();
//        // In production, replace with your actual frontend origins
//        config.setAllowedOriginPatterns(List.of("http://localhost:*", "https://*.bayr.com"));
//        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
//        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
//        config.setExposedHeaders(List.of("X-Request-ID"));
//        config.setAllowCredentials(true);
//        config.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }
}

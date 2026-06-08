package com.libseat.config;

import tools.jackson.databind.ObjectMapper;
import com.libseat.common.ErrorCode;
import com.libseat.common.Result;
import com.libseat.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 认证接口：完全公开
                .requestMatchers(
                        "/v1/auth/register",
                        "/v1/auth/activate",
                        "/v1/auth/login",
                        "/v1/auth/refresh",
                        "/v1/auth/password/reset-request",
                        "/v1/auth/password/reset",
                        "/v1/auth/email/confirm"
                ).permitAll()
                // 座位/图书馆浏览：匿名可读
                .requestMatchers(HttpMethod.GET, "/v1/seats", "/v1/seats/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/libraries", "/v1/libraries/**").permitAll()
                // 后台管理：仅 ADMIN
                .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                // 其余均需登录
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    ErrorCode code = (ErrorCode) request.getAttribute(JwtAuthenticationFilter.ATTR_JWT_ERROR);
                    if (code == null) code = ErrorCode.NOT_LOGGED_IN;
                    writeJson(response, Result.fail(code));
                })
                .accessDeniedHandler((request, response, e) ->
                    writeJson(response, Result.fail(ErrorCode.FORBIDDEN))
                )
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 禁用 Spring Security 的内存用户自动装配，消除启动时的 generated password 警告 */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> { throw new UnsupportedOperationException("JWT-only auth"); };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("http://localhost:[*]"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private void writeJson(HttpServletResponse response, Result<?> body) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

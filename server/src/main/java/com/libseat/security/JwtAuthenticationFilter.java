package com.libseat.security;

import com.libseat.common.ErrorCode;
import com.libseat.repository.UserRepository;
import com.libseat.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** SecurityConfig 的 AuthenticationEntryPoint 读取此属性来区分错误类型 */
    public static final String ATTR_JWT_ERROR = "JWT_ERROR_CODE";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            if (jwtService.isRefreshToken(token)) {
                request.setAttribute(ATTR_JWT_ERROR, ErrorCode.TOKEN_INVALID);
                chain.doFilter(request, response);
                return;
            }

            UUID userId = jwtService.extractUserId(token);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findById(userId).ifPresent(user -> {
                    var auth = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        } catch (ExpiredJwtException e) {
            request.setAttribute(ATTR_JWT_ERROR, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            request.setAttribute(ATTR_JWT_ERROR, ErrorCode.TOKEN_INVALID);
        }

        chain.doFilter(request, response);
    }
}

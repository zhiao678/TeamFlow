package com.teamflow.backend.common.auth;

import com.teamflow.backend.common.enums.AccountStatus;
import com.teamflow.backend.entity.User;
import com.teamflow.backend.mapper.UserMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserMapper userMapper) {
        this.jwtTokenService = jwtTokenService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Long userId = jwtTokenService.parseUserId(header.substring(7));
                User user = userMapper.selectById(userId);
                if (user != null && !AccountStatus.DISABLED.name().equals(user.getAccountStatus())) {
                    String role = user.getPlatformRole() == null ? "USER" : user.getPlatformRole();
                    CurrentUserContext.set(userId);
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            )
                    );
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // SecurityFilterChain will reject protected routes without a valid user.
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}

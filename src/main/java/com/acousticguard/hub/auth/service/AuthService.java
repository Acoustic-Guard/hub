package com.acousticguard.hub.auth.service;

import com.acousticguard.hub.auth.dto.LoginRequest;
import com.acousticguard.hub.auth.dto.LoginResponse;
import com.acousticguard.hub.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Service for authentication operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login request
     * @return the login response with token
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String token = jwtService.generateToken(request.username());

        // Get user role from authentication
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        log.info("User {} authenticated successfully with role {}", request.username(), role);

        return new LoginResponse(token, request.username(), role);
    }
}

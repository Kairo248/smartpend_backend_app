package com.spendSmart.backend.service;

import com.spendSmart.backend.dto.auth.*;
import com.spendSmart.backend.entity.RefreshToken;
import com.spendSmart.backend.entity.User;
import com.spendSmart.backend.repository.RefreshTokenRepository;
import com.spendSmart.backend.repository.UserRepository;
import com.spendSmart.backend.security.JwtTokenProvider;
import com.spendSmart.backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public JwtAuthenticationResponse signUp(SignUpRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email is already taken!");
        }

        // Create new user
        User user = User.builder()
                .name(signUpRequest.getName())
                .email(signUpRequest.getEmail())
                .passwordHash(passwordEncoder.encode(signUpRequest.getPassword()))
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateJwtToken(savedUser.getEmail());
        String refreshToken = generateRefreshToken(savedUser);

        return new JwtAuthenticationResponse(accessToken, refreshToken);
    }

    @Transactional
    public JwtAuthenticationResponse signIn(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateJwtToken(authentication);
        
        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String refreshToken = generateRefreshToken(user);

        return new JwtAuthenticationResponse(accessToken, refreshToken);
    }

    @Transactional
    public JwtAuthenticationResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found!"));

        if (refreshToken.isExpired() || refreshToken.getIsRevoked()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token was expired or revoked. Please make a new signin request");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateJwtToken(user.getEmail());
        String newRefreshToken = generateRefreshToken(user);

        // Revoke old refresh token
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        return new JwtAuthenticationResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(refreshToken)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }

    private String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
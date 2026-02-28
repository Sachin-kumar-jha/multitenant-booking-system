package com.ticket.auth.service;

import com.ticket.auth.dto.*;
import com.ticket.auth.entity.RefreshToken;
import com.ticket.auth.entity.User;
import com.ticket.auth.entity.User.UserRole;
import com.ticket.auth.entity.User.UserStatus;
import com.ticket.auth.repository.RefreshTokenRepository;
import com.ticket.auth.repository.UserRepository;
import com.ticket.common.exception.BaseException;
import com.ticket.common.exception.ResourceNotFoundException;
import com.ticket.common.security.JwtService;
import com.ticket.common.tenant.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final MeterRegistry meterRegistry;

    @Value("${jwt.expiration:900000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenExpiration;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String tenantId = TenantContext.requireCurrentTenant();

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Create user
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);
        log.info("User registered: {} for tenant {}", savedUser.getId(), tenantId);

        // Record metric
        meterRegistry.counter("auth.registration.success", "tenant", tenantId).increment();

        return createAuthResponse(savedUser, tenantId);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String tenantId = TenantContext.requireCurrentTenant();

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountSuspendedException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            meterRegistry.counter("auth.login.failure", "tenant", tenantId, "reason", "invalid_password").increment();
            throw new InvalidCredentialsException();
        }

        // Update last login time
        userRepository.updateLastLoginTime(user.getId(), Instant.now());

        log.info("User logged in: {} for tenant {}", user.getId(), tenantId);
        meterRegistry.counter("auth.login.success", "tenant", tenantId).increment();

        return createAuthResponse(user, tenantId);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tenantId = TenantContext.requireCurrentTenant();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!refreshToken.isActive()) {
            meterRegistry.counter("auth.refresh.failure", "tenant", tenantId, "reason", "token_inactive").increment();
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", refreshToken.getUserId()));

        // Revoke old token
        refreshToken.setRevokedAt(Instant.now());

        // Create new tokens
        String newRefreshTokenString = createRefreshToken(user.getId());
        refreshToken.setReplacedBy(newRefreshTokenString);
        refreshTokenRepository.save(refreshToken);

        log.info("Token refreshed for user: {} tenant: {}", user.getId(), tenantId);
        meterRegistry.counter("auth.refresh.success", "tenant", tenantId).increment();

        return createAuthResponse(user, tenantId, newRefreshTokenString);
    }

    @Transactional
    public void logout(String userId) {
        String tenantId = TenantContext.requireCurrentTenant();
        refreshTokenRepository.revokeAllUserTokens(userId, Instant.now());
        log.info("User logged out: {} from tenant {}", userId, tenantId);
    }

    private AuthResponse createAuthResponse(User user, String tenantId) {
        String refreshTokenString = createRefreshToken(user.getId());
        return createAuthResponse(user, tenantId, refreshTokenString);
    }

    private AuthResponse createAuthResponse(User user, String tenantId, String refreshToken) {
        String accessToken = jwtService.generateToken(user.getId(), tenantId, user.getRole().name());

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setFirstName(user.getFirstName());
        userInfo.setLastName(user.getLastName());
        userInfo.setRole(user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(accessTokenExpiration / 1000);
        response.setUser(userInfo);

        return response;
    }

    private String createRefreshToken(String userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID().toString());
        refreshToken.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS));

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    // Custom exceptions
    public static class EmailAlreadyExistsException extends BaseException {
        public EmailAlreadyExistsException(String email) {
            super("Email already registered: " + email, HttpStatus.CONFLICT, "EMAIL_EXISTS");
        }
    }

    public static class InvalidCredentialsException extends BaseException {
        public InvalidCredentialsException() {
            super("Invalid email or password", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
    }

    public static class InvalidTokenException extends BaseException {
        public InvalidTokenException(String message) {
            super(message, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
        }
    }

    public static class AccountSuspendedException extends BaseException {
        public AccountSuspendedException() {
            super("Account is suspended", HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED");
        }
    }
}

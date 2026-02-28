package com.ticket.auth.service;

import com.ticket.auth.dto.AuthResponse;
import com.ticket.auth.dto.LoginRequest;
import com.ticket.auth.dto.RegisterRequest;
import com.ticket.auth.entity.RefreshToken;
import com.ticket.auth.entity.User;
import com.ticket.auth.entity.User.UserRole;
import com.ticket.auth.entity.User.UserStatus;
import com.ticket.auth.repository.RefreshTokenRepository;
import com.ticket.auth.repository.UserRepository;
import com.ticket.common.security.JwtService;
import com.ticket.common.tenant.TenantContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encoded_password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setStatus(UserStatus.ACTIVE);

        // Setup TenantContext mock
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::requireCurrentTenant).thenReturn("tenant1");
        tenantContextMock.when(TenantContext::getTenantId).thenReturn("tenant1");
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(anyString(), anyString(), anyString())).thenReturn("access_token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        AuthResponse result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access_token");
        assertThat(result.getUser().getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject duplicate email registration")
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(AuthService.EmailAlreadyExistsException.class)
            .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("Should login successfully")
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString(), anyString())).thenReturn("access_token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        doNothing().when(userRepository).updateLastLoginTime(anyString(), any());

        AuthResponse result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access_token");
        assertThat(result.getRefreshToken()).isNotNull();
    }

    @Test
    @DisplayName("Should reject invalid credentials")
    void shouldRejectInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong_password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthService.InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should reject login for suspended user")
    void shouldRejectSuspendedUser() {
        testUser.setStatus(UserStatus.SUSPENDED);
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthService.AccountSuspendedException.class);
    }

    @Test
    @DisplayName("Should reject login for non-existent user")
    void shouldRejectNonExistentUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthService.InvalidCredentialsException.class);
    }
}

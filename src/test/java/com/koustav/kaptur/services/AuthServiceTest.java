package com.koustav.kaptur.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.koustav.kaptur.dto.AuthResponse;
import com.koustav.kaptur.dto.LoginRequest;
import com.koustav.kaptur.dto.RegisterRequest;
import com.koustav.kaptur.model.CustomUserDetails;
import com.koustav.kaptur.model.RefreshToken;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.enums.AuthProvider;
import com.koustav.kaptur.repository.UserRepository;
import com.koustav.kaptur.security.JwtUtils;

/**
 * Unit tests for AuthService.
 * Tests authentication, registration logic using Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UUID testKptId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "googleClientId", "test-client-id");

        testKptId = UUID.fromString("0192f3a4-5678-9abc-def0-123456789abc");
        testUser = User.builder()
                .kptId(testKptId)
                .email("john@example.com")
                .name("John Doe")
                .provider(AuthProvider.LOCAL)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // authenticateUser() Tests
    // ==========================================

    @Test
    @DisplayName("authenticateUser - valid credentials returns AuthResponse with token and safe user fields")
    void authenticateUser_success() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                userDetails, "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtUtils.generateJwtToken(mockAuth)).thenReturn("fake-jwt-token");
        when(refreshTokenService.createRefreshToken(testKptId))
                .thenReturn(RefreshToken.builder().token("fake-refresh-token").build());

        // Act
        AuthResponse response = authService.authenticateUser(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getAccessToken());
        assertEquals("fake-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(testKptId, response.getKptId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getName());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateJwtToken(mockAuth);
    }

    @Test
    @DisplayName("authenticateUser - invalid credentials throws BadCredentialsException")
    void authenticateUser_badCredentials_throws() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrong@example.com");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class,
                () -> authService.authenticateUser(loginRequest));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtUtils);
    }

    // ==========================================
    // registerUser() Tests
    // ==========================================

    @Test
    @DisplayName("registerUser - new email registers successfully")
    void registerUser_success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setName("New User");

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encodedPassword");

        // Simulate save: User gets kptId assigned in createUser() via UuidCreator
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getKptId() == null) {
                user.setKptId(UUID.fromString("0192f3a4-0000-0000-0000-000000000001"));
            }
            return user;
        });

        // Act
        String result = authService.registerUser(request);

        // Assert
        assertEquals("User registered successfully!", result);

        verify(userRepository).existsByEmail("newuser@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registerUser - duplicate email throws RuntimeException")
    void registerUser_duplicateEmail_throws() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setName("Existing User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.registerUser(request));

        assertTrue(exception.getMessage().contains("Email is already in use"));

        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }
}

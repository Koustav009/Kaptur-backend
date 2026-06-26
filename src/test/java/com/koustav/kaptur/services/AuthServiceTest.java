package com.koustav.kaptur.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.repository.UserRepository;
import com.koustav.kaptur.security.JwtUtils;

/**
 * Unit tests for AuthService.
 * Tests authentication, registration, and Google login logic using Mockito mocks.
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

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Set the @Value field that would normally be injected by Spring
        ReflectionTestUtils.setField(authService, "googleClientId", "test-client-id");
    }

    @AfterEach
    void tearDown() {
        // Clear security context after each test to avoid leaks
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // authenticateUser() Tests
    // ==========================================

    @Test
    @DisplayName("authenticateUser - valid credentials returns AuthResponse with token")
    void authenticateUser_success() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                "john@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtUtils.generateJwtToken(mockAuth)).thenReturn("fake-jwt-token");

        // Act
        AuthResponse response = authService.authenticateUser(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());

        // Verify SecurityContext was set
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        // Verify interactions
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateJwtToken(mockAuth);
    }

    @Test
    @DisplayName("authenticateUser - invalid credentials throws BadCredentialsException")
    void authenticateUser_badCredentials_throws() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
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

        // Simulate the double-save pattern:
        // First save sets the ID (simulating @GeneratedValue)
        // Second save returns the user with kptId set
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(1L); // Simulate auto-generated ID
            }
            return user;
        });

        // Act
        String result = authService.registerUser(request);

        // Assert
        assertEquals("User registered successfully!", result);

        // Verify: existsByEmail check, password encoding, and two save calls
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository, times(2)).save(any(User.class));
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

        // Verify: checked for existing email but never tried to save
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }
}

package com.koustav.kaptur.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koustav.kaptur.dto.AuthResponse;
import com.koustav.kaptur.dto.LoginRequest;
import com.koustav.kaptur.dto.RegisterRequest;
import com.koustav.kaptur.security.CustomUserDetailsService;
import com.koustav.kaptur.security.JwtUtils;
import com.koustav.kaptur.services.AuthService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AuthController using MockMvc.
 * 
 * Security filters are disabled since /auth/** endpoints are public.
 * We mock the AuthService to isolate controller-level logic.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    // Mock security dependencies so AuthTokenFilter can be instantiated
    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ==========================================
    // POST /auth/register Tests
    // ==========================================

    @Test
    @DisplayName("POST /auth/register - valid request returns 201 with success message")
    void registerUser_success() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setName("New User");

        when(authService.registerUser(any(RegisterRequest.class)))
                .thenReturn("User registered successfully!");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered successfully!"));
    }

    @Test
    @DisplayName("POST /auth/register - duplicate email returns 400")
    void registerUser_duplicateEmail_returns400() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setName("Existing User");

        when(authService.registerUser(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Error: Email is already in use!"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error: Email is already in use!"));
    }

    // ==========================================
    // POST /auth/login Tests
    // ==========================================

    @Test
    @DisplayName("POST /auth/login - valid credentials returns 200 with JWT token")
    void authenticateUser_success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(authService.authenticateUser(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("fake-jwt-token"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fake-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/login - invalid credentials returns 401")
    void authenticateUser_badCredentials_returns401() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@example.com");
        request.setPassword("wrongpassword");

        when(authService.authenticateUser(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    // ==========================================
    // GET /auth/test Tests
    // ==========================================

    @Test
    @DisplayName("GET /auth/test - returns 200 with health check message")
    void testEndpoint_returnsOk() throws Exception {
        mockMvc.perform(get("/auth/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth endpoint is working"));
    }
}

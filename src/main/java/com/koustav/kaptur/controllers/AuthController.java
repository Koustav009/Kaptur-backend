package com.koustav.kaptur.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koustav.kaptur.dto.AuthResponse;
import com.koustav.kaptur.dto.GoogleLoginRequest;
import com.koustav.kaptur.dto.LoginRequest;
import com.koustav.kaptur.dto.RefreshTokenRequest;
import com.koustav.kaptur.dto.RegisterRequest;
import com.koustav.kaptur.services.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @RestController means this class handles HTTP requests (like POST, GET).
 *                 Controllers are the "entry point" to our
 *                 application. @RequestMapping("/auth") means all URLs in this
 *                 class start with /auth.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * @PostMapping("/login") handles POST requests to /auth/login.
     * 
     * @RequestBody converts the incoming JSON to a LoginRequest object.
     *              ResponseEntity allows us to return status codes (200 OK, 400 Bad
     *              Request, etc.).
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());
        try {
            // We delegate the heavy work to the Service layer.
            AuthResponse response = authService.authenticateUser(loginRequest);
            log.info("Login successful for email: {}", loginRequest.getEmail());
            return ResponseEntity.ok(response); // Returns 200 OK with the JWT token.
        } catch (Exception e) {
            log.error("Login failed for email: {}", loginRequest.getEmail(), e);
            throw e;
        }
    }

    /**
     * @PostMapping("/google") handles Native Google Login from Flutter. We receive
     * the user data from Flutter, verify it, and issue our backend's JWT.
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        // Exchange Google data for our backend's JWT token.
        AuthResponse response = authService.googleLogin2(request);
        return ResponseEntity.ok(response);
    }

    /**
     * @PostMapping({"/refresh"}) allows the frontend to obtain a brand new Access
     * Token when their old one expires without making the user type their password
     * again.
     * 
     * NOTE FOR LEARNERS: When this endpoint is called, our backend checks the
     * database (`check and update the role from db`) to make sure the user's role
     * and permissions are fully up to date on the newly issued access token!
     */
    @PostMapping({ "/refresh" })
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh attempt received");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * @PostMapping("/register") handles POST requests to /auth/register.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for email: {}", registerRequest.getEmail());
        try {
            // We call the Service to save the user in the database.
            String result = authService.registerUser(registerRequest);
            log.info("Registration successful for email: {}", registerRequest.getEmail());
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Registration failed for email: {}", registerRequest.getEmail(), e);
            throw e;
        }
    }

    /**
     * A simple test endpoint to check if the /auth path is open.
     */
    @GetMapping("/test")
    public String test() {
        log.debug("Test endpoint called");
        return "Auth endpoint is working";
    }
}

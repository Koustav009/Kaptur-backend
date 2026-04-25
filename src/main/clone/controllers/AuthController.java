package com.fotoowl.clone.controllers;

import com.fotoowl.clone.dto.AuthResponse;
import com.fotoowl.clone.dto.GoogleLoginRequest;
import com.fotoowl.clone.dto.LoginRequest;
import com.fotoowl.clone.dto.RegisterRequest;
import com.fotoowl.clone.services.AuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @RestController means this class handles HTTP requests (like POST, GET).
 * Controllers are the "entry point" to our application.
 * @RequestMapping("/auth") means all URLs in this class start with /auth.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * @PostMapping("/login") handles POST requests to /auth/login.
     * @RequestBody converts the incoming JSON to a LoginRequest object.
     * ResponseEntity allows us to return status codes (200 OK, 400 Bad Request, etc.).
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // We delegate the heavy work to the Service layer.
        AuthResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response); // Returns 200 OK with the JWT token.
    }

    /**
     * @PostMapping("/google") handles Native Google Login from Flutter.
     * We receive the user data from Flutter, verify it, and issue our backend's JWT.
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        // Exchange Google data for our backend's JWT token.
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * @PostMapping("/register") handles POST requests to /auth/register.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        // We call the Service to save the user in the database.
        String result = authService.registerUser(registerRequest);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    /**
     * A simple test endpoint to check if the /auth path is open.
     */
    @GetMapping("/test")
    public String test() {
        return "Auth endpoint is working";
    }
}

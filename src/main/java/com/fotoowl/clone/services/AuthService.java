package com.fotoowl.clone.services;

import com.fotoowl.clone.dto.AuthResponse;
import com.fotoowl.clone.dto.LoginRequest;
import com.fotoowl.clone.dto.RegisterRequest;
import com.fotoowl.clone.model.AuthProvider;
import com.fotoowl.clone.model.User;
import com.fotoowl.clone.repository.UserRepository;
import com.fotoowl.clone.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @Service is where the "Business Logic" lives.
 * It's where we do calculations, check database, and perform logic.
 * We inject repositories and other tools (like PasswordEncoder) here.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager; // Handles login checks
    private final UserRepository userRepository; // Access to DB
    private final PasswordEncoder passwordEncoder; // Encrypts passwords
    private final JwtUtils jwtUtils; // Generates tokens

    /**
     * This method handles the logic when a user tries to log in.
     */
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        // 1. We ask the AuthenticationManager to check the email and password.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        // 2. If valid, we store the authentication info in the Security Context.
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 3. We generate a JWT token so the user stays logged in for their next requests.
        String jwt = jwtUtils.generateJwtToken(authentication);

        return new AuthResponse(jwt);
    }

    /**
     * This method handles the logic to create a new user account.
     */
    public String registerUser(RegisterRequest registerRequest) {
        // 1. Check if the email is already registered.
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // 2. Build a new User object from the registration request.
        // We MUST hash the password using PasswordEncoder before saving it.
        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .provider(AuthProvider.LOCAL) // Local sign-up
                .build();

        // 3. Save the new user to the MySQL database.
        userRepository.save(user);

        return "User registered successfully!";
    }
}

package com.fotoowl.clone.services;

import com.fotoowl.clone.dto.AuthResponse;
import com.fotoowl.clone.dto.LoginRequest;
import com.fotoowl.clone.dto.RegisterRequest;
import com.fotoowl.clone.dto.GoogleLoginRequest;
import com.fotoowl.clone.model.User;
import com.fotoowl.clone.model.enums.AuthProvider;
import com.fotoowl.clone.repository.UserRepository;
import com.fotoowl.clone.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @Service is where the "Business Logic" lives.
 *          It's where we do calculations, check database, and perform logic.
 *          We inject repositories and other tools (like PasswordEncoder) here.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager; // Handles login checks
    private final UserRepository userRepository; // Access to DB
    private final PasswordEncoder passwordEncoder; // Encrypts passwords
    private final JwtUtils jwtUtils; // Generates tokens

    private void createUser(User user) {
        // get next KPT id ;
        // then set it;

        Long nextSeq = userRepository.getNextKptSequence();
        // EX : KPT0000001
        String kptId = String.format("KPT%07d", nextSeq);

        user.setKptId(kptId);

        userRepository.save(user);
    }

    /**
     * This method handles the logic when a user tries to log in with email and
     * password.
     */
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        // 1. We ask the AuthenticationManager to check the email and password.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        // 2. If valid, we store the authentication info in the Security Context.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. We generate a JWT token so the user stays logged in for their next
        // requests.
        String jwt = jwtUtils.generateJwtToken(authentication);

        return new AuthResponse(jwt);
    }

    /**
     * This method handles Native Google Login from Flutter.
     * It checks if the Google user already exists in our DB, saves them if not,
     * and returns a JWT for them to use in our app.
     */
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        // 1. Search for a user with this email in our database.
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // If the user already exists, update their name and photo just in case.
            user.setName(request.getName());
            user.setImageUrl(request.getPhotoUrl());
            userRepository.save(user);
        } else {
            // 2. If the user doesn't exist, this is their first time logging in!
            // We create a new User object with Google as the provider.
            user = User.builder()
                    .email(request.getEmail())
                    .name(request.getName())
                    .imageUrl(request.getPhotoUrl())
                    .provider(AuthProvider.GOOGLE)
                    .providerId(request.getId())
                    .build();
            createUser(user);
        }

        // 3. Generate our application's JWT for this user.
        String token = jwtUtils.generateTokenFromUsername(user.getEmail());
        return new AuthResponse(token);
    }

    /**
     * This method handles the logic to create a new user account with email and
     * password.
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
        createUser(user);

        return "User registered successfully!";
    }
}

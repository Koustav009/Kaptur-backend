package com.koustav.kaptur.services;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.koustav.kaptur.dto.AuthResponse;
import com.koustav.kaptur.dto.GoogleLoginRequest;
import com.koustav.kaptur.dto.LoginRequest;
import com.koustav.kaptur.dto.RefreshTokenRequest;
import com.koustav.kaptur.dto.RegisterRequest;
import com.koustav.kaptur.model.CustomUserDetails;
import com.koustav.kaptur.model.RefreshToken;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.enums.AuthProvider;
import com.koustav.kaptur.model.enums.SystemRole;
import com.koustav.kaptur.repository.UserRepository;
import com.koustav.kaptur.security.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager; // Handles login checks
    private final UserRepository userRepository; // Access to DB
    private final PasswordEncoder passwordEncoder; // Encrypts passwords
    private final JwtUtils jwtUtils; // Generates tokens
    private final RefreshTokenService refreshTokenService; // Manages refresh tokens

    // Your Google Client ID from Google Cloud Console.
    // This MUST match the clientId used in your Flutter app.
    @Value("${google.client-id}")
    private String googleClientId;

    private User createUser(User user) {
        log.debug("Creating new user with email: {}", user.getEmail());
        // save user first to get the id
        // userRepository.save(user);

        // String kptId = String.format("KPT%07d", user.getId());
        // String kptId = UUID.randomUUID().toString();
        UUID uuid = UuidCreator.getTimeOrderedEpoch();

        user.setKptId(uuid);

        User savedUser = userRepository.save(user);
        log.info("User created successfully with kptId: {}", uuid);
        return savedUser;
    }

    /**
     * This method handles the logic when a user tries to log in with email and
     * password.
     */
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user with email: {}", loginRequest.getEmail());
        try {
            // 1. We ask the AuthenticationManager to check the email and password.
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                            loginRequest.getPassword()));

            // 2. If valid, we store the authentication info in the Security Context.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. We generate a short-lived access token and a long-lived refresh token.
            String token = jwtUtils.generateJwtToken(authentication);
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = customUserDetails.getUser();

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getKptId());
            String roleStr = user.getRole() != null ? user.getRole().name() : SystemRole.USER.name();

            log.info("User authenticated successfully: {}", loginRequest.getEmail());
            return AuthResponse.builder()
                    .accessToken(token)
                    .refreshToken(refreshToken.getToken())
                    .kptId(user.getKptId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .imageUrl(user.getImageUrl())
                    .role(roleStr)
                    .build();
        } catch (Exception e) {
            log.error("Authentication failed for email: {}", loginRequest.getEmail(), e);
            throw e;
        }
    }

    /**
     * This method handles Native Google Login from Flutter. It checks if the Google
     * user already exists in our DB, saves them if not, and returns a JWT for them
     * to use in our app.
     */
    public AuthResponse googleLogin2(GoogleLoginRequest request) {
        log.info("Google login attempt with token");

        // ---------------------------------------------------------------
        // STEP 1: Build a GoogleIdTokenVerifier.
        // This verifier hits Google's public JWKS endpoint to validate
        // the token signature, expiry, and audience (your client ID).
        // ---------------------------------------------------------------
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), // standard
                                                                                                   // HTTP
                // transport
                GsonFactory.getDefaultInstance())
                        // "audience" must be YOUR app's Google Client ID.
                        // Google will reject tokens not intended for your app.
                        .setAudience(Collections.singletonList(googleClientId)).build();

        // ---------------------------------------------------------------
        // STEP 2: Verify the raw ID token string from the Flutter app.
        // If invalid (expired, wrong audience, bad signature), returns null.
        // ---------------------------------------------------------------
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(request.getId());
        } catch (Exception e) {
            log.error("Google token verification failed", e);
            throw new RuntimeException("Google token verification failed: " + e.getMessage());
        }

        if (idToken == null) {
            log.warn("Invalid or expired Google ID token");
            throw new RuntimeException("Invalid or expired Google ID token");
        }

        // ---------------------------------------------------------------
        // STEP 3: Extract user info from the verified token's payload.
        // No need to trust what the Flutter app sends — read it from here.
        // ---------------------------------------------------------------
        Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        String googleId = payload.getSubject(); // Google's unique user ID ("sub" claim)

        log.debug("Google token verified for email: {}", email);

        // ---------------------------------------------------------------
        // STEP 4: Find or create the user in your database.
        // ---------------------------------------------------------------
        Optional<User> userOptional = userRepository.findByProviderId(googleId);
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(email);
            log.debug("Existing user found with email: {}", email);
        } else {
            log.debug("Existing user found with providerId: {}", email);
        }
        User user;

        if (userOptional.isPresent()) {
            // User already exists — update their profile info from Google

            user = userOptional.get();
            user.setName(name);
            user.setImageUrl(pictureUrl);
            user = userRepository.save(user);
        } else {
            // First time login — create a new user
            log.info("Creating new Google user with email: {}", email);
            User tmp_user = User.builder().email(email).name(name).imageUrl(pictureUrl)
                    .provider(AuthProvider.GOOGLE).providerId(googleId).build();
            user = createUser(tmp_user);
        }

        // ---------------------------------------------------------------
        // STEP 5: Issue your own app's access token and refresh token and return to Flutter.
        // ---------------------------------------------------------------
        String token = jwtUtils.generateTokenFromUser(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getKptId());
        String roleStr = user.getRole() != null ? user.getRole().name() : SystemRole.USER.name();

        log.info("Google login successful for user: {}", email);
        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken.getToken())
                .kptId(user.getKptId())
                .email(user.getEmail())
                .name(user.getName())
                .imageUrl(user.getImageUrl())
                .role(roleStr)
                .build();
    }

    /**
     * Handles refreshing an Access Token using a valid Refresh Token.
     * 
     * EDUCATIONAL NOTE FOR LEARNERS:
     * When using a refresh token to get a new access token, we perform a live database lookup
     * (`check and update the role from db`).
     * 
     * Why check the DB here?
     * Because while the user was holding their old access token, an admin might have promoted them
     * to `SUPER_ADMIN` or demoted them. By checking the database right here when issuing the new
     * access token, any role changes immediately take effect without requiring the user to log out and back in!
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        log.info("Processing refresh token request");

        // 1. Look up the refresh token in the database
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database! Please login again."));

        // 2. Verify that the refresh token hasn't expired
        refreshTokenService.verifyExpiration(refreshToken);

        // 3. Fetch the latest User entity from the database (`check and update the role from db`)
        UUID kptId = refreshToken.getUser().getKptId();
        User user = userRepository.findByKptId(kptId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + kptId));

        // 4. Check/verify the user's latest role from the database entity
        SystemRole currentDbRole = user.getRole();
        log.info("Checked role from DB for user kptId {}: {}", kptId, currentDbRole);

        // 5. Generate a brand new Access Token embedding the updated role from DB
        String newAccessToken = jwtUtils.generateTokenFromUser(user);

        log.info("Successfully generated new access token using refresh token for user kptId: {}", kptId);

        // 6. Return the updated token details to the client
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .kptId(user.getKptId())
                .email(user.getEmail())
                .name(user.getName())
                .imageUrl(user.getImageUrl())
                .role(currentDbRole.name())
                .build();
    }

    /**
     * This method handles the logic to create a new user account with email and
     * password.
     */
    public String registerUser(RegisterRequest registerRequest) {
        log.info("Registration attempt for email: {}", registerRequest.getEmail());
        // 1. Check if the email is already registered.
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Email already in use: {}", registerRequest.getEmail());
            throw new RuntimeException("Error: Email is already in use!");
        }

        // 2. Build a new User object from the registration request.
        // We MUST hash the password using PasswordEncoder before saving it.
        User user = User.builder().name(registerRequest.getName()).email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .provider(AuthProvider.LOCAL) // Local
                // sign-up
                .build();

        // 3. Save the new user to the MySQL database.
        createUser(user);
        log.info("User registered successfully: {}", registerRequest.getEmail());

        return "User registered successfully!";
    }
}

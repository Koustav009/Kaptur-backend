package com.koustav.kaptur.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.uuid.UuidCreator;
import com.koustav.kaptur.model.RefreshToken;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.repository.RefreshTokenRepository;
import com.koustav.kaptur.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class responsible for managing the lifecycle of Refresh Tokens:
 * creation, verification, and deletion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    // Reads refresh token duration from application.properties or uses 7 days default (604,800,000 ms)
    @Value("${app.jwtRefreshExpirationMs:604800000}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new refresh token for the given user, or updates their existing token.
     * We use @Transactional to ensure database consistency when updating or creating records.
     */
    @Transactional
    public RefreshToken createRefreshToken(UUID kptId) {
        log.debug("Creating refresh token for user kptId: {}", kptId);
        
        // 1. Fetch the user from the database using their UUID
        User user = userRepository.findByKptId(kptId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + kptId));

        // 2. Check if a refresh token already exists for this user.
        // If it does, we update its token string and expiry date to reuse the existing database row.
        // If not, we build a brand new RefreshToken entity.
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);
        RefreshToken refreshToken;
        if (existingToken.isPresent()) {
            refreshToken = existingToken.get();
            refreshToken.setToken(UuidCreator.getTimeOrderedEpoch().toString());
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
            log.debug("Updated existing refresh token record for user kptId: {}", kptId);
        } else {
            refreshToken = RefreshToken.builder()
                    .tokenId(UuidCreator.getTimeOrderedEpoch())
                    .user(user)
                    .token(UuidCreator.getTimeOrderedEpoch().toString())
                    .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                    .build();
            log.debug("Created new refresh token record for user kptId: {}", kptId);
        }

        // 3. Save the token record to the database
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Looks up a refresh token by its string representation.
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verifies whether the provided refresh token has expired.
     * If expired, it deletes the token from the database and throws an exception.
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        // Compare token's expiryDate with the current system timestamp
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            log.warn("Refresh token expired for user kptId: {}", token.getUser().getKptId());
            // Token has expired! Delete it from the database so it cannot be reused.
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new login request.");
        }
        return token;
    }

    /**
     * Deletes the refresh token belonging to a specific user (e.g., during logout).
     */
    @Transactional
    public int deleteByUserId(UUID kptId) {
        User user = userRepository.findByKptId(kptId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + kptId));
        return refreshTokenRepository.deleteByUser(user);
    }
}

package com.koustav.kaptur.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import com.koustav.kaptur.model.RefreshToken;
import com.koustav.kaptur.model.User;

/**
 * Repository interface for managing RefreshToken records in the database.
 * Spring Data JPA automatically generates the SQL queries for these methods based on their names.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Finds a refresh token entity by the token string sent by the client.
    Optional<RefreshToken> findByToken(String token);

    // Finds the existing refresh token associated with a specific user, if any.
    Optional<RefreshToken> findByUser(User user);

    // @Modifying tells Spring this query modifies the database (DELETE/UPDATE).
    // Deletes the refresh token belonging to a specific user (useful when logging out or replacing tokens).
    @Modifying
    int deleteByUser(User user);
}

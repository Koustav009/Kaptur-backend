package com.koustav.kaptur.model;

import java.util.UUID;

import com.koustav.kaptur.model.enums.AuthProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Entity tells Spring Data JPA that this class represents a table in the
 *         database.
 * @Table(name = "users") specifies the name of the table.
 * @Data (from Lombok) automatically creates Getters, Setters, toString, etc.
 */
@Entity
@Table(name = "USERS", indexes = { @Index(name = "idx_email", columnList = "email"), })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // Primary key — a UUIDv7 (time-ordered) assigned at creation.
    // UUIDv7 embeds a Unix timestamp, so it is monotonically increasing and
    // indexes efficiently in B-tree indexes (unlike random UUIDv4).
    @Id
    @Column(name = "kpt_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID kptId;

    // unique = true means two users cannot have the same email.
    @Column(nullable = false, unique = true)
    private String email;

    // password is kept null for Google users as they don't have a local password.
    @Column
    private String password;

    @Column(nullable = false)
    private String name;

    // Stores the enum value as a String ("LOCAL" or "GOOGLE") in the DB.
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    // The unique ID provided by Google (called 'sub' in OAuth).
    @Column(unique = true)
    private String providerId;

    // URL to the user's profile picture from Google.
    private String imageUrl;
}

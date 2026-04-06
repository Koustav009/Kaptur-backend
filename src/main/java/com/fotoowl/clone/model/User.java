package com.fotoowl.clone.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Entity tells Spring Data JPA that this class represents a table in the database.
 * @Table(name = "users") specifies the name of the table.
 * @Data (from Lombok) automatically creates Getters, Setters, toString, etc.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // This is the Primary Key of our table, it will auto-increment.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private String providerId;

    // URL to the user's profile picture from Google.
    private String imageUrl;
}

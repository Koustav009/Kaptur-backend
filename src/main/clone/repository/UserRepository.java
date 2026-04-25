package com.fotoowl.clone.repository;

import com.fotoowl.clone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repositories are interfaces used to perform CRUD (Create, Read, Update,
 * Delete)
 * operations on the database without writing SQL.
 * JpaRepository provides methods like save(), findById(), delete(), etc.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring generates the SQL for this automatically based on the method name!
    Optional<User> findByEmail(String email);

    // Checks if a user already exists with this email.
    Boolean existsByEmail(String email);

    @Query(value = "SELECT nextval('kpt_id_seq')", nativeQuery = true)
    Long getNextKptSequence();
}

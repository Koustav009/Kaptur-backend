package com.fotoowl.clone.security;

import com.fotoowl.clone.model.User;
import com.fotoowl.clone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Spring Security needs an interface called UserDetailsService to load user info
 * from the database during authentication. This is our implementation.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * This method is called by Spring Security's AuthenticationManager.
     * It looks for a user by email in our 'users' table.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Find user in the database.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email : " + email));

        // 2. Return a UserDetails object that Spring Security understands.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword() != null ? user.getPassword() : "", // Google users have empty passwords
                new ArrayList<>() // Empty list of roles/authorities for now
        );
    }
}

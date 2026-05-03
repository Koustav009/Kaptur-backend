package com.koustav.kaptur.security;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koustav.kaptur.model.CustomUserDetails;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.repository.UserRepository;

import java.util.ArrayList;

/**
 * Spring Security needs an interface called UserDetailsService to load user
 * info from the database during authentication. This is our implementation.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * This method is called by Spring Security's AuthenticationManager. It looks
     * for a user by email in our 'users' table.
     */
    @Override
    @Transactional // is it ok to use transectional here ?? is it required ?
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 1. Find user in the database.
        User user;

        user = userRepository.findByEmail(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email : " + identifier));

        // 2. Return a UserDetails object that Spring Security understands.
        return new CustomUserDetails(user);
    }
}
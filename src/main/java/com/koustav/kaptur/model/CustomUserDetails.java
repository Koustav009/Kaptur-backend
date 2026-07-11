package com.koustav.kaptur.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.Setter;

/**
 * Adapter class that bridges our custom User JPA entity to Spring Security's UserDetails interface.
 * This lets Spring Security inspect our user's email, password, and roles/authorities during authentication.
 */
@Getter
@Setter
public class CustomUserDetails implements UserDetails {

    private User user;
    private Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        // NOTE FOR LEARNERS:
        // We convert our User entity's enum role into a Spring Security GrantedAuthority.
        // Spring Security convention expects role names to start with "ROLE_" (e.g. ROLE_USER, ROLE_SUPER_ADMIN).
        ArrayList<GrantedAuthority> auths = new ArrayList<>();
        if (user != null && user.getRole() != null) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        } else {
            auths.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        this.authorities = auths;
    }

    public UUID getKptId() {
        return user.getKptId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // keep email as username
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}

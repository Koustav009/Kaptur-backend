package com.koustav.kaptur.model;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private String kptId;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.kptId = user.getKptId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.authorities = new ArrayList<>();
    }

    public String getKptId() {
        return kptId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getUsername() {
        return email; // keep email as username
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // other methods → return true
}
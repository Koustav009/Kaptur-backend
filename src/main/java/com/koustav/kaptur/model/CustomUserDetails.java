package com.koustav.kaptur.model;

import java.util.ArrayList;
import java.util.Collection;

import org.checkerframework.checker.index.qual.SearchIndexBottom;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
public class CustomUserDetails implements UserDetails {

    // private String kptId;
    // private String email;
    // private String password;
    private User user;
    private Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.authorities = new ArrayList<>();
    }

    public String getKptId() {
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

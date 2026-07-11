package com.koustav.kaptur.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.koustav.kaptur.model.enums.SystemRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This Filter runs EVERY time we get an API request. It's the "Security Guard"
 * checking if the user has a valid ID card (JWT).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    /**
     * Logic to intercept the request and check the JWT token in the header.
     * 
     * NOTE FOR LEARNERS ON REFRESH TOKEN ARCHITECTURE:
     * In this filter, we CHECK ONLY THE SIGNATURE and validity of the Access Token in-memory using JwtUtils.
     * We DO NOT execute a database query (`userRepository.findByKptId(...)`) on every API request.
     * This keeps our backend stateless, highly scalable, and blazing fast.
     * 
     * When the short-lived access token expires and the user uses their Refresh Token to get a new access token,
     * THAT is when our service checks and updates the role from the database!
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Extract the token from the "Authorization" header.
            String jwt = parseJwt(request);

            // 2. If token exists and passes signature check...
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // 3. Get the user's kptId (subject) directly from the token.
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                // 4. Extract the user's role directly from the JWT claims without hitting the DB (`check only signature`).
                String role = jwtUtils.getRoleFromJwtToken(jwt);
                log.debug("JWT signature validated for user: {} with role: {}", username, role);

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (role != null && !role.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                } else {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + SystemRole.USER.name()));
                }

                // 5. Build Spring Security User object in memory from extracted claims.
                User userDetails = new org.springframework.security.core.userdetails.User(username,
                        "", authorities);

                // 6. Create an authentication object.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Tell Spring Security: "This user is authenticated!".
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (jwt != null) {
                log.warn("Invalid or expired JWT token received");
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        // 7. Let the request continue to the Controller.
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts "Bearer <token>" from the HTTP Header.
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // Removes "Bearer " prefix
        }

        return null;
    }
}

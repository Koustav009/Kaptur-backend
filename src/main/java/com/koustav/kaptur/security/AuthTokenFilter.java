package com.koustav.kaptur.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This Filter runs EVERY time we get an API request. It's the "Security Guard"
 * checking if the user has a valid ID card (JWT).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Logic to intercept the request and check the JWT token in the header.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Extract the token from the "Authorization" header.
            String jwt = parseJwt(request);

            // 2. If token exists and is valid...
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // 3. Get the user's email from the token.
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                log.debug("JWT validated for user: {}", username);

                // 4. Load the user from the database.
                User userDetails = new org.springframework.security.core.userdetails.User(username, "",
                        new ArrayList<>());

                // 5. Create an authentication object.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Tell Spring Security: "This user is authenticated!".
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (jwt != null) {
                log.warn("Invalid JWT token received");
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

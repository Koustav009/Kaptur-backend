package com.koustav.kaptur.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * This is the central configuration for Spring Security. It defines which URLs
 * are public and which are private.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuthTokenFilter authTokenFilter;
    // private final OAuth2AuthenticationSuccessHandler
    // oAuth2AuthenticationSuccessHandler;

    /**
     * Tells Spring Security how to find users (using UserDetailsService) and how to
     * check passwords (using BCrypt).
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        log.info("DaoAuthenticationProvider is called");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * This tool is used in AuthService to check credentials during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Password encoder to hash passwords before storing in DB.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The main security rules of our application.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 1. Disable CSRF (not needed for JWT-based REST APIs).
        http.csrf(csrf -> csrf.disable())

                // 1b. Enable CORS using the CorsConfigurationSource bean from CorsConfig.
                // This runs at the security-filter level so preflight OPTIONS requests are
                // answered (with CORS headers) before the authentication rules below.
                .cors(Customizer.withDefaults())

                // 2. Set Session policy to STATELESS (don't use cookies, use tokens).
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. Set public and private URLs.
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Preflight never 401s.
                        .requestMatchers("/auth/**").permitAll() // Anyone can login and
                                                                 // register.
                        .requestMatchers("/oauth2/**").permitAll() // Google OAuth path is public.
                        .requestMatchers("/v3/api-docs/**").permitAll() // Swagger docs
                        .requestMatchers("/swagger-ui/**").permitAll() // Swagger UI
                        .requestMatchers("/swagger-ui.html").permitAll() // Swagger UI
                        .requestMatchers("/tusd/**").permitAll() // Swagger docs
                        .anyRequest().authenticated() // ALL other URLs require a valid JWT!
                )
                // 4. Handle authentication errors (Instead of redirecting to Google Login).
                .exceptionHandling(
                        exception -> exception.authenticationEntryPoint((request, response, authException) -> {
                            // We return 401 Unauthorized instead of a 302 Redirect.
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("Error: Unauthorized - Please Login First");
                        }));
        // 5. Configure Google OAuth login.
        // .oauth2Login(oauth2 ->
        // oauth2.successHandler(oAuth2AuthenticationSuccessHandler));

        // 6. Connect our user database check.
        http.authenticationProvider(authenticationProvider());

        // 7. Add our JWT Guard (Filter) before the standard username/password guard.
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

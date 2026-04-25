package com.fotoowl.clone.security;

import com.fotoowl.clone.model.User;
import com.fotoowl.clone.model.enums.AuthProvider;
import com.fotoowl.clone.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

/**
 * This class handles what happens AFTER a successful Google Login.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // 1. Get user details from Google.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String imageUrl = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");

        // 2. Save or Update the user in our local 'users' database.
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setName(name); // Update name from Google
            user.setImageUrl(imageUrl); // Update picture from Google
            userRepository.save(user);
        } else {
            // First time this Google user is logging in.
            user = User.builder()
                    .email(email)
                    .name(name)
                    .imageUrl(imageUrl)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .build();
            userRepository.save(user);
        }

        // 3. Generate a JWT token for our backend.
        String token = jwtUtils.generateTokenFromUsername(email);

        // 4. Redirect the browser to the Frontend (React/Angular) with the token in the
        // URL.
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

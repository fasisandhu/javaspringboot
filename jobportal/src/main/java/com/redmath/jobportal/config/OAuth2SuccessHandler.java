package com.redmath.jobportal.config;

import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;

    public OAuth2SuccessHandler(JwtEncoder jwtEncoder, UserRepository userRepository) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // Cast to OAuth2AuthenticationToken to get the original OAuth2User
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            log.error("Authentication is not OAuth2AuthenticationToken");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid authentication type");
            return;
        }


        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauth2Token.getPrincipal();

        // Get attributes from the OAuth2User
        Map<String, Object> attributes = oauth2User.getAttributes();
        log.info("OAuth2 user attributes: {}", attributes);

        if (attributes == null || attributes.isEmpty()) {
            log.error("OAuth2 user attributes are null or empty");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 provider didn't return user attributes");
            return;
        }

        // Get email from attributes
        String email = getEmailFromAttributes(attributes);
        if (email == null || email.isEmpty()) {
            log.error("Email not found in OAuth2 user attributes");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth2 provider");
            return;
        }

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getRole() != null) {
                generateAndSendJwt(response, email);
                return;
            }
        } else {
            // Create new user if doesn't exist
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(getNameFromAttributes(attributes));
            userRepository.save(newUser);
        }

        getRedirectStrategy().sendRedirect(request, response, "/auth/select-role");
    }

    private String getEmailFromAttributes(Map<String, Object> attributes) {
        // Try different attribute names based on common OAuth2 providers
        String email = (String) attributes.get("email");
        if (email == null) {
            email = (String) attributes.get("sub");
        }
        if (email == null) {
            email = (String) attributes.get("login");
        }
        if (email == null) {
            email = (String) attributes.get("preferred_username");
        }
        return email;
    }

    private String getNameFromAttributes(Map<String, Object> attributes) {
        String name = (String) attributes.get("name");
        if (name == null) {
            name = (String) attributes.get("login");
        }
        return name != null ? name : "Unknown";
    }

    private void generateAndSendJwt(HttpServletResponse response, String username) throws IOException {
        // Find the user to include role information in JWT
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isEmpty()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User not found");
            return;
        }

        User user = userOptional.get();
        long expirySeconds = 3600;

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder().subject(username).expiresAt(Instant.now().plusSeconds(expirySeconds));

        // Add role to JWT claims if available
        if (user.getRole() != null) {
            claimsBuilder.claim("role", user.getRole().name());
        }

        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));

        String json = String.format("{\"token_type\":\"Bearer\",\"access_token\":\"%s\",\"expires_in\":%d}", jwt.getTokenValue(), expirySeconds);

        response.setContentType("application/json");
        response.getWriter().print(json);
    }
}
package com.redmath.jobportal.config;

import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    // Add frontend URL configuration
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            log.error("Authentication is not OAuth2AuthenticationToken");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid authentication type");
            return;
        }

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauth2Token.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();

        if (attributes == null || attributes.isEmpty()) {
            log.error("OAuth2 user attributes are null or empty");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 provider didn't return user attributes");
            return;
        }

        String email = getEmailFromAttributes(attributes);
        if (email == null || email.isEmpty()) {
            log.error("Email not found in OAuth2 user attributes");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth2 provider");
            return;
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            // Create new user if doesn't exist
            user = new User();
            user.setEmail(email);
            user.setName(getNameFromAttributes(attributes));
            user.setProvider(AuthProvider.GOOGLE);
            userRepository.save(user);
        }

        // Generate JWT and redirect to frontend
        redirectToFrontendWithToken(response, email, user);
    }

    private void redirectToFrontendWithToken(HttpServletResponse response, String email, User user) throws IOException {
        long expirySeconds = 3600;

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .subject(email)
                .expiresAt(Instant.now().plusSeconds(expirySeconds));

        if (user.getRole() != null) {
            claimsBuilder.claim("role", user.getRole().name());
            claimsBuilder.claim("role_selected", true);
        } else {
            claimsBuilder.claim("role_selected", false);
        }

        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));

        // Build redirect URL with token and role selection status
        String redirectUrl = buildRedirectUrl(jwt.getTokenValue(), user.getRole() != null);

        log.info("Redirecting OAuth2 user to frontend: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private String buildRedirectUrl(String token, boolean roleSelected) {
        if (roleSelected) {
            return String.format("%s/auth/success?token=%s", frontendUrl, token);
        } else {
            return String.format("%s/auth/role-selection?token=%s", frontendUrl, token);
        }
    }

    private String getEmailFromAttributes(Map<String, Object> attributes) {
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

    private void generateAndSendJwt(HttpServletResponse response, String email, User user) throws IOException {
        long expirySeconds = 3600;

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .subject(email)
                .expiresAt(Instant.now().plusSeconds(expirySeconds));

        // Add role to JWT claims if available, otherwise add a temporary claim
        if (user.getRole() != null) {
            claimsBuilder.claim("role", user.getRole().name());
            claimsBuilder.claim("role_selected", true);
        } else {
            claimsBuilder.claim("role_selected", false);
        }

        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));

        String json = String.format(
                "{\"token_type\":\"Bearer\",\"access_token\":\"%s\",\"expires_in\":%d,\"role_selected\":%s}",
                jwt.getTokenValue(),
                expirySeconds,
                user.getRole() != null
        );

        response.setContentType("application/json");
        response.getWriter().print(json);
    }
}
package com.redmath.jobportal.auth.services;

import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class RoleSelectionService {

    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;

    public RoleSelectionService(UserRepository userRepository, JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public Map<String, Object> generateJwtResponse(String email, User user) {
        long expirySeconds = 3600;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .claim("role", user.getRole().name())
                .expiresAt(Instant.now().plusSeconds(expirySeconds))
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));

        Map<String, Object> response = new HashMap<>();
        response.put("token_type", "Bearer");
        response.put("access_token", jwt.getTokenValue());
        response.put("expires_in", expirySeconds);

        return response;
    }

    public User updateUserRole(String email, String role) {
        User user = getUserByEmail(email);
        if (user != null) {
            try {
                Role selectedRole = Role.valueOf(role.toUpperCase(Locale.ENGLISH));
                user.setRole(selectedRole);
                userRepository.save(user);
            } catch (IllegalArgumentException e) {
                return null; // Invalid role
            }
        }
        return user;
    }
}


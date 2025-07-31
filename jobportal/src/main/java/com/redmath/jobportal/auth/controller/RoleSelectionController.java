package com.redmath.jobportal.auth.controller;

import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class RoleSelectionController {

    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;

    public RoleSelectionController(UserRepository userRepository, JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
    }
    @GetMapping("/user-role")
    public ResponseEntity<Map<String, Object>> getUserRole(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = getEmailFromPrincipal(principal);
        if (email == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Email not found"));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("role", user.getRole() != null ? user.getRole().name() : null);
        response.put("role_selected", user.getRole() != null);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/roles")
    public ResponseEntity<Map<String, Object>> getAvailableRoles(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = getEmailFromPrincipal(principal);
        if (email == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Email not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("roles", Arrays.asList("APPLICANT", "EMPLOYER"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/select-role")
    public ResponseEntity<Map<String, Object>> selectRole(
            @RequestBody Map<String, String> request,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String role = request.get("role");
        if (role == null || role.trim().isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Role is required"));
        }

        String email = getEmailFromPrincipal(principal);
        if (email == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Email not found"));
        }

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        try {
            Role selectedRole = Role.valueOf(role.toUpperCase());
            user.setRole(selectedRole);
            userRepository.save(user);

            Map<String, Object> tokenResponse = generateJwtResponse(email, user);
            return ResponseEntity.ok(tokenResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid role selected"));
        }
    }

    private Map<String, Object> generateJwtResponse(String email, User user) {
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

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) principal).getPrincipal();
            return oauth2User.getAttribute("email");
        }
        return principal.getName();
    }
}
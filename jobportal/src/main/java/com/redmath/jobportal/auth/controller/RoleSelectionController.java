package com.redmath.jobportal.auth.controller;

import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.services.RoleSelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class RoleSelectionController {

    private final RoleSelectionService roleSelectionService;

    public RoleSelectionController(RoleSelectionService roleSelectionService) {
        this.roleSelectionService = roleSelectionService;
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

        User user = roleSelectionService.getUserByEmail(email);
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

        User user = roleSelectionService.getUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
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
            return ResponseEntity.status(404).body(Map.of("error", "Email not found"));
        }

        User user = roleSelectionService.updateUserRole(email, role);
        if (user == null) {
            return ResponseEntity.status(400).body(Map.of("error", "User not found or invalid role"));
        }

        Map<String, Object> tokenResponse = roleSelectionService.generateJwtResponse(email, user);
        return ResponseEntity.ok(tokenResponse);
    }

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) principal).getPrincipal();
            return oauth2User.getAttribute("email");
        }
        return principal.getName();
    }
}

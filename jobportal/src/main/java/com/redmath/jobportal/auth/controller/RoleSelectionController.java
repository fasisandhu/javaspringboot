package com.redmath.jobportal.auth.controller;

import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;

@Controller
public class RoleSelectionController {

    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;

    public RoleSelectionController(UserRepository userRepository,JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
    }

    @GetMapping("/auth/select-role")
    public String showRoleSelection(Principal principal, Model model) {
        if (principal == null) {
            // If no principal, redirect to login
            return "redirect:/oauth2/authorization/google";
        }

        String email = null;

        if (principal instanceof OAuth2AuthenticationToken)
        {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) principal).getPrincipal();
            email = oauth2User.getAttribute("email");
        }
        else
        {
            email = principal.getName();
        }

        if (email == null) {
            return "redirect:/oauth2/authorization/google";
        }

        // Add email and roles to the model
        model.addAttribute("email", email);
        model.addAttribute("roles", Arrays.asList("APPLICANT", "EMPLOYER"));

        return "role-selection";
    }

    @PostMapping("/auth/select-role")
    public void processRoleSelection(
            @RequestParam("role") String role,
            Principal principal,
            HttpServletResponse response) throws IOException {

        if (principal == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        String email = getEmailFromPrincipal(principal);
        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found");
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            Role selectedRole = Role.valueOf(role.toUpperCase());
            user.setRole(selectedRole);
            userRepository.save(user);

            // Generate JWT and return as JSON
            generateJwtResponse(response, email, user);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid role selected");
        }
    }

    private void generateJwtResponse(HttpServletResponse response, String email, User user) throws IOException {
        long expirySeconds = 3600;

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .claim("role", user.getRole().name())
                .expiresAt(Instant.now().plusSeconds(expirySeconds))
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));

        String json = String.format(
                "{\"token_type\":\"Bearer\",\"access_token\":\"%s\",\"expires_in\":%d}",
                jwt.getTokenValue(),
                expirySeconds
        );

        response.setContentType("application/json");
        response.getWriter().print(json);
    }

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) principal).getPrincipal();
            return oauth2User.getAttribute("email");
        }
        return principal.getName();
    }
}
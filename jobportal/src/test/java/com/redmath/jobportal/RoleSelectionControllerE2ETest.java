package com.redmath.jobportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class RoleSelectionControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User userWithoutRole;
    private User userWithRole;

    @BeforeEach
    void setUp() {
        // Create user without role (OAuth2 user)
        userWithoutRole = User.builder()
                .email("oauth@example.com")
                .name("OAuth User")
                .password("dummy")
                .role(null) // No role selected yet
                .provider(AuthProvider.GOOGLE)
                .build();
        userWithoutRole = userRepository.save(userWithoutRole);

        // Create user with role already selected
        userWithRole = User.builder()
                .email("existing@example.com")
                .name("Existing User")
                .password("dummy")
                .role(Role.EMPLOYER)
                .provider(AuthProvider.GOOGLE)
                .build();
        userWithRole = userRepository.save(userWithRole);
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void testGetUserRole_UserWithoutRole() throws Exception {
        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("oauth@example.com"))
                .andExpect(jsonPath("$.role").isEmpty())
                .andExpect(jsonPath("$.role_selected").value(false));
    }

    @Test
    @WithMockUser(username = "existing@example.com")
    void testGetUserRole_UserWithRole() throws Exception {
        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("existing@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYER"))
                .andExpect(jsonPath("$.role_selected").value(true));
    }

    @Test
    void testGetUserRole_NotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com")
    void testGetUserRole_UserNotFound() throws Exception {
        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @WithMockUser(username = "existing@example.com")
    void testGetAvailableRoles_Success() throws Exception {
        mockMvc.perform(get("/api/auth/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("existing@example.com"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("APPLICANT"))
                .andExpect(jsonPath("$.roles[1]").value("EMPLOYER"));
    }

    @Test
    void testGetAvailableRoles_NotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com")
    void testGetAvailableRoles_UserNotFound() throws Exception {
        mockMvc.perform(get("/api/auth/roles"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void testSelectRole_Success_Applicant() throws Exception {
        Map<String, String> request = Map.of("role", "APPLICANT");

        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.expires_in").value(3600));

        // Verify role was saved in database
        var updatedUser = userRepository.findByEmail("oauth@example.com").orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(Role.APPLICANT);
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void testSelectRole_Success_Employer() throws Exception {
        Map<String, String> request = Map.of("role", "EMPLOYER");

        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.expires_in").value(3600));

        // Verify role was saved in database
        var updatedUser = userRepository.findByEmail("oauth@example.com").orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(Role.EMPLOYER);
    }

    @Test
    @WithMockUser(username = "existing@example.com")
    void testSelectRole_UpdateExistingRole() throws Exception {
        // User already has EMPLOYER role, change to APPLICANT
        Map<String, String> request = Map.of("role", "APPLICANT");

        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());

        // Verify role was updated in database
        var updatedUser = userRepository.findByEmail("existing@example.com").orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(Role.APPLICANT);
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void testSelectRole_InvalidRole() throws Exception {
        Map<String, String> request = Map.of("role", "INVALID_ROLE");

        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found or invalid role"));

        // Verify role was not changed in database
        var user = userRepository.findByEmail("oauth@example.com").orElseThrow();
        assertThat(user.getRole()).isNull();
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void testSelectRole_EmptyRole() throws Exception {
        Map<String, String> request = Map.of("role", "");

        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role is required"));

        // Verify role was not changed in database
        var user = userRepository.findByEmail("oauth@example.com").orElseThrow();
        assertThat(user.getRole()).isNull();
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void testSelectRole_MissingRole() throws Exception {
        Map<String, String> request = Map.of("other", "value");

        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role is required"));
    }

    @Test
    void testSelectRole_NotAuthenticated() throws Exception {
        Map<String, String> request = Map.of("role", "APPLICANT");

        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com")
    void testSelectRole_UserNotFound() throws Exception {
        Map<String, String> request = Map.of("role", "APPLICANT");

        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found or invalid role"));
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void testSelectRole_CaseInsensitive() throws Exception {
        Map<String, String> request = Map.of("role", "applicant"); // lowercase

        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());

        // Verify role was saved correctly
        var updatedUser = userRepository.findByEmail("oauth@example.com").orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(Role.APPLICANT);
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void testSelectRole_MissingCsrfToken() throws Exception {
        Map<String, String> request = Map.of("role", "APPLICANT");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify role was not changed
        var user = userRepository.findByEmail("oauth@example.com").orElseThrow();
        assertThat(user.getRole()).isNull();
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void testCompleteRoleSelectionFlow() throws Exception {
        // 1. Check initial state - no role
        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role_selected").value(false));

        // 2. Get available roles
        mockMvc.perform(get("/api/auth/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());

        // 3. Select a role
        Map<String, String> request = Map.of("role", "EMPLOYER");
        mockMvc.perform(post("/api/auth/select-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());

        // 4. Verify role was selected
        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYER"))
                .andExpect(jsonPath("$.role_selected").value(true));

        // 5. Verify in database
        var user = userRepository.findByEmail("oauth@example.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.EMPLOYER);
    }
}
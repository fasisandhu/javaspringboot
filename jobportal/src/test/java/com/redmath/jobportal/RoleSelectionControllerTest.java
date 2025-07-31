package com.redmath.jobportal;

import com.redmath.jobportal.auth.controller.RoleSelectionController;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleSelectionController.class)
public class RoleSelectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtEncoder jwtEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User sampleUser;
    private User userWithoutRole;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("John Doe")
                .role(Role.APPLICANT)
                .provider(AuthProvider.LOCAL)
                .build();

        userWithoutRole = User.builder()
                .id(2L)
                .email("norole@example.com")
                .name("Jane Doe")
                .role(null)
                .provider(AuthProvider.GOOGLE)
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testGetUserRole_Success() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("APPLICANT"))
                .andExpect(jsonPath("$.role_selected").value(true));
    }

    @Test
    @WithMockUser(username = "norole@example.com")
    void testGetUserRole_UserWithoutRole() throws Exception {
        when(userRepository.findByEmail("norole@example.com")).thenReturn(Optional.of(userWithoutRole));

        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("norole@example.com"))
                .andExpect(jsonPath("$.role").isEmpty())
                .andExpect(jsonPath("$.role_selected").value(false));
    }

    @Test
    @WithMockUser(username = "notfound@example.com")
    void testGetUserRole_UserNotFound() throws Exception {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void testGetUserRole_NotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/user-role"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("oauth2/authorization")));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testGetAvailableRoles_Success() throws Exception {
        mockMvc.perform(get("/api/auth/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("APPLICANT"))
                .andExpect(jsonPath("$.roles[1]").value("EMPLOYER"));
    }

    @Test
    void testGetAvailableRoles_NotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/roles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("oauth2/authorization")));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testSelectRole_Success() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userWithoutRole));
        when(userRepository.save(any(User.class))).thenReturn(userWithoutRole);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock-jwt-token");
        when(jwtEncoder.encode(any())).thenReturn(mockJwt);

        Map<String, String> request = new HashMap<>();
        request.put("role", "APPLICANT");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.expires_in").value(3600));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testSelectRole_InvalidRole() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userWithoutRole));

        Map<String, String> request = new HashMap<>();
        request.put("role", "INVALID_ROLE");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid role selected"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testSelectRole_EmptyRole() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("role", "");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role is required"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testSelectRole_NullRole() throws Exception {
        Map<String, String> request = new HashMap<>();

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role is required"));
    }

    @Test
    @WithMockUser(username = "notfound@example.com")
    void testSelectRole_UserNotFound() throws Exception {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        Map<String, String> request = new HashMap<>();
        request.put("role", "APPLICANT");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void testSelectRole_NotAuthenticated() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("role", "APPLICANT");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("oauth2/authorization")));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testSelectRole_EmployerRole() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userWithoutRole));
        when(userRepository.save(any(User.class))).thenReturn(userWithoutRole);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock-jwt-token");
        when(jwtEncoder.encode(any())).thenReturn(mockJwt);

        Map<String, String> request = new HashMap<>();
        request.put("role", "EMPLOYER");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token").value("mock-jwt-token"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testSelectRole_CaseInsensitive() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userWithoutRole));
        when(userRepository.save(any(User.class))).thenReturn(userWithoutRole);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock-jwt-token");
        when(jwtEncoder.encode(any())).thenReturn(mockJwt);

        Map<String, String> request = new HashMap<>();
        request.put("role", "applicant");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("mock-jwt-token"));
    }
}
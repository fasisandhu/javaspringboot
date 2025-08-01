package com.redmath.jobportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redmath.jobportal.auth.dtos.RegisterRequest;
import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class UserControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Test data from liquibase will be available
        // employer@x.com and applicant@x.com already exist
    }

    @Test
    void testShowRegisterForm() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void testRegisterUser_Success_Employer() throws Exception {
        String uniqueEmail = "newemployer@example.com";

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("name", "New Employer")
                        .param("email", uniqueEmail)
                        .param("password", "password123")
                        .param("role", "EMPLOYER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("success", "Registration successful! Please login."));

        // Verify user was saved in database
        var savedUser = userRepository.findByEmail(uniqueEmail);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getName()).isEqualTo("New Employer");
        assertThat(savedUser.get().getRole()).isEqualTo(Role.EMPLOYER);
        assertThat(savedUser.get().getProvider()).isEqualTo(AuthProvider.LOCAL);
        // Password should be encoded, not plain text
        assertThat(savedUser.get().getPassword()).isNotEqualTo("password123");
    }

    @Test
    void testRegisterUser_Success_Applicant() throws Exception {
        String uniqueEmail = "newapplicant@example.com";

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("name", "New Applicant")
                        .param("email", uniqueEmail)
                        .param("password", "password123")
                        .param("role", "APPLICANT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("success", "Registration successful! Please login."));

        // Verify user was saved in database
        var savedUser = userRepository.findByEmail(uniqueEmail);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getName()).isEqualTo("New Applicant");
        assertThat(savedUser.get().getRole()).isEqualTo(Role.APPLICANT);
        assertThat(savedUser.get().getProvider()).isEqualTo(AuthProvider.LOCAL);
    }

    @Test
    void testRegisterUser_DuplicateEmail() throws Exception {
        // Try to register with existing email from liquibase
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("name", "Duplicate User")
                        .param("email", "employer@x.com") // Already exists in liquibase
                        .param("password", "password123")
                        .param("role", "EMPLOYER"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("name", "Duplicate User"))
                .andExpect(model().attribute("email", "employer@x.com"))
                .andExpect(model().attribute("role", "EMPLOYER"));

        // Verify no new user was created
        var allUsers = userRepository.findAll();
        var duplicateUsers = allUsers.stream()
                .filter(user -> "Duplicate User".equals(user.getName()))
                .toList();
        assertThat(duplicateUsers).isEmpty();
    }

    @Test
    void testRegisterUser_InvalidRole() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("name", "Test User")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("role", "INVALID_ROLE"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Invalid role selected"))
                .andExpect(model().attribute("name", "Test User"))
                .andExpect(model().attribute("email", "test@example.com"));

        // Verify no user was created
        var savedUser = userRepository.findByEmail("test@example.com");
        assertThat(savedUser).isEmpty();
    }

    @Test
    void testRegisterApi_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("API User")
                .email("apiuser@example.com")
                .password("password123")
                .role(Role.APPLICANT)
                .provider(AuthProvider.LOCAL)
                .build();

        mockMvc.perform(post("/auth/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful! Please login."));

        // Verify user was saved in database
        var savedUser = userRepository.findByEmail("apiuser@example.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getName()).isEqualTo("API User");
        assertThat(savedUser.get().getRole()).isEqualTo(Role.APPLICANT);
        assertThat(savedUser.get().getProvider()).isEqualTo(AuthProvider.LOCAL);
    }

    @Test
    void testRegisterApi_DuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Duplicate API User")
                .email("employer@x.com") // Already exists in liquibase
                .password("password123")
                .role(Role.EMPLOYER)
                .provider(AuthProvider.LOCAL)
                .build();

        mockMvc.perform(post("/auth/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Verify no new user was created
        var allUsers = userRepository.findAll();
        var duplicateUsers = allUsers.stream()
                .filter(user -> "Duplicate API User".equals(user.getName()))
                .toList();
        assertThat(duplicateUsers).isEmpty();
    }

    @Test
    void testRegisterApi_InvalidJson() throws Exception {
        String invalidJson = "{ \"name\": \"Test\", \"email\": }"; // Malformed JSON

        mockMvc.perform(post("/auth/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterForm_MissingCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("name", "Test User")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("role", "APPLICANT"))
                .andExpect(status().isForbidden());

        // Verify no user was created
        var savedUser = userRepository.findByEmail("test@example.com");
        assertThat(savedUser).isEmpty();
    }

    @Test
    void testRegisterApi_MissingCsrfToken() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("CSRF Test User")
                .email("csrftest@example.com")
                .password("password123")
                .role(Role.APPLICANT)
                .provider(AuthProvider.LOCAL)
                .build();

        mockMvc.perform(post("/auth/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify no user was created
        var savedUser = userRepository.findByEmail("csrftest@example.com");
        assertThat(savedUser).isEmpty();
    }

    @Test
    void testRegisterForm_EmptyFields() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("name", "")
                        .param("email", "")
                        .param("password", "")
                        .param("role", "APPLICANT"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        // This test depends on your validation logic
        // The controller might handle empty fields differently
    }

    @Test
    void testUserCountAfterRegistration() throws Exception {
        // Count existing users (from liquibase)
        long initialCount = userRepository.count();
        assertThat(initialCount).isEqualTo(2); // employer@x.com and applicant@x.com

        // Register new user
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("name", "Count Test User")
                        .param("email", "count@example.com")
                        .param("password", "password123")
                        .param("role", "APPLICANT"))
                .andExpect(status().is3xxRedirection());

        // Verify count increased
        long finalCount = userRepository.count();
        assertThat(finalCount).isEqualTo(initialCount + 1);
    }
}
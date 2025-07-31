package com.redmath.jobportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redmath.jobportal.auth.controller.UserController;
import com.redmath.jobportal.auth.dtos.RegisterRequest;
import com.redmath.jobportal.auth.exceptions.UserRegistrationException;
import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.services.UserService;
import com.redmath.jobportal.config.ApiSecurityConfiguration;
import com.redmath.jobportal.exceptions.DuplicateEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { ApiSecurityConfiguration.class })
)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("API Register - Success")
    void testRegisterApi_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
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
    }

    @Test
    @DisplayName("API Register - Duplicate Email")
    void testRegisterApi_DuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("password123")
                .role(Role.EMPLOYER)
                .provider(AuthProvider.LOCAL)
                .build();

        doThrow(new DuplicateEmailException("A user with this email already exists"))
                .when(userService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Form Register - Success")
    void testRegisterForm_Success() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("name", "John Doe")
                        .param("email", "john@example.com")
                        .param("password", "password123")
                        .param("role", "APPLICANT")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @DisplayName("Form Register - Duplicate Email Error")
    void testRegisterForm_DuplicateEmail() throws Exception {
        doThrow(new DuplicateEmailException("A user with this email already exists"))
                .when(userService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/auth/register")
                        .param("name", "Jane Doe")
                        .param("email", "jane@example.com")
                        .param("password", "password123")
                        .param("role", "EMPLOYER")
                        .param("provider", "LOCAL")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("Form Register - Invalid Role")
    void testRegisterForm_InvalidRole() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("name", "Jane Doe")
                        .param("email", "jane@example.com")
                        .param("password", "password123")
                        .param("role", "INVALID_ROLE")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Invalid role selected"))
                .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("Form Register - UserRegistrationException")
    void testRegisterForm_UserRegistrationException() throws Exception {
        doThrow(new UserRegistrationException("Internal Error"))
                .when(userService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/auth/register")
                        .param("name", "Jane Doe")
                        .param("email", "jane@example.com")
                        .param("password", "password123")
                        .param("role", "APPLICANT")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Registration failed. Please try again later."))
                .andExpect(view().name("register"));
    }
}
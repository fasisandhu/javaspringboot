package com.redmath.jobportal;

import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.CustomUserDetails;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.auth.services.CustomUserDetailsService;
import com.redmath.jobportal.exceptions.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded-password")
                .name("John Doe")
                .role(Role.APPLICANT)
                .provider(AuthProvider.LOCAL)
                .build();
    }

    @Test
    void testLoadUserByUsername_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertTrue(userDetails instanceof CustomUserDetails);
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("nonexistent@example.com");
        });
    }

    @Test
    void testLoadUserByUsername_DatabaseError() {
        when(userRepository.findByEmail(anyString())).thenThrow(new RuntimeException("Database error"));

        assertThrows(UserNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("test@example.com");
        });
    }
}
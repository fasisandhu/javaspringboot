package com.redmath.jobportal;

import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.auth.services.UserService;
import com.redmath.jobportal.auth.dtos.RegisterRequest;
import com.redmath.jobportal.exceptions.DuplicateEmailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest sampleRegisterRequest;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleRegisterRequest = RegisterRequest.builder()
                .name("John Doe")
                .email("test@example.com")
                .password("password123")
                .role(Role.APPLICANT)
                .provider(AuthProvider.LOCAL)
                .build();

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
    void testRegister_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        assertDoesNotThrow(() -> userService.register(sampleRegisterRequest));
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void testRegister_UserAlreadyExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sampleUser));

        assertThrows(DuplicateEmailException.class, () -> {
            userService.register(sampleRegisterRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_EmptyEmail() {
        sampleRegisterRequest.setEmail("");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userService.register(sampleRegisterRequest));
    }

    @Test
    void testRegister_NullEmail() {
        sampleRegisterRequest.setEmail(null);
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userService.register(sampleRegisterRequest));
    }
}
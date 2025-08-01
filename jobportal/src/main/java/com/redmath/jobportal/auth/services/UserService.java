package com.redmath.jobportal.auth.services;

import com.redmath.jobportal.auth.dtos.RegisterRequest;
import com.redmath.jobportal.auth.exceptions.UserRegistrationException;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.exceptions.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
//@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest request) {
//        log.info("Attempting to register user with email: {}", request.getEmail());

        // Check for existing user
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
//            log.warn("Registration failed: email already exists - {}", request.getEmail());
            throw new DuplicateEmailException("A user with this email already exists");
        }

        try {
            User user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .role(request.getRole())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .provider(request.getProvider())
                    .build();

            userRepository.save(user);
//            log.info("User registered successfully: {}", request.getEmail());

        } catch (DataIntegrityViolationException e) {
//            log.error("Database constraint violation during registration for email: {}", request.getEmail(), e);
            throw new DuplicateEmailException("A user with this email already exists");
        } catch (Exception e) {
//            log.error("Unexpected error during registration for email: {}", request.getEmail(), e);
            throw new UserRegistrationException("Registration failed due to an internal error", e);
        }
    }
}
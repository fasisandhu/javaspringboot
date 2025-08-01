package com.redmath.jobportal.auth.services;

import com.redmath.jobportal.auth.exceptions.OAuth2ProcessingException;
import com.redmath.jobportal.auth.exceptions.UserCreationException;
import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.redmath.jobportal.auth.model.CustomUserDetails;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
//@Slf4j
@Service
@RequiredArgsConstructor

public class CustomUserDetailsService extends DefaultOAuth2UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
//        log.info("Attempting to load user by email: {}", email);

        try {
            return userRepository.findByEmail(email)
                    .map(user -> {
//                        log.info("User found successfully: {}", email);
                        return new CustomUserDetails(user);
                    })
                    .orElseThrow(() -> {
//                        log.warn("User not found with email: {}", email);
                        return new UserNotFoundException("User not found with email: " + email);
                    });
        } catch (DataAccessException e) {
//            log.error("Database error while loading user: {}", email, e);
            throw new UserNotFoundException("Unable to load user due to database error", e);
        } catch (Exception e) {
//            log.error("Unexpected error while loading user: {}", email, e);
            throw new UserNotFoundException("Unable to load user", e);
        }
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
//        log.info("Processing OAuth2 user request");

        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attributes = oAuth2User.getAttributes();
//            log.debug("OAuth2 attributes received: {}", attributes);

            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");

            if (email == null || email.trim().isEmpty()) {
//                log.error("Email not provided in OAuth2 attributes");
                throw new OAuth2ProcessingException("Email is required for OAuth2 authentication");
            }

//            log.info("Processing OAuth2 user with email: {}", email);

            try {
                Optional<User> userOptional = userRepository.findByEmail(email);
                User user = userOptional.orElseGet(() -> createOAuth2User(email, name));

//                log.info("OAuth2 user processed successfully: {}", email);

                // Pass attributes to CustomUserDetails constructor
                if (user.getRole() == null) {
//                    log.info("User requires role selection: {}", email);
                    return new CustomUserDetails(user, true, attributes);
                }

                return new CustomUserDetails(user, attributes);

            } catch (DataAccessException e) {
//                log.error("Database error during OAuth2 user processing for email: {}", email, e);
                throw new OAuth2ProcessingException("Database error during OAuth2 authentication", e);
            }

        } catch (OAuth2AuthenticationException e) {
//            log.error("OAuth2 authentication failed", e);
            throw e;
        } catch (OAuth2ProcessingException e) {
//            log.error("OAuth2 processing failed", e);
            throw new OAuth2AuthenticationException(e.getMessage());
        } catch (Exception e) {
//            log.error("Unexpected error during OAuth2 user processing", e);
            throw new OAuth2AuthenticationException("OAuth2 authentication failed");
        }
    }

    private User createOAuth2User(String email, String name) {
//        log.info("Creating new OAuth2 user: {}", email);

        try {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name != null ? name : "OAuth2 User");
            newUser.setPassword(""); // No password for OAuth users
            newUser.setProvider(AuthProvider.GOOGLE);
            // Note: Role will be set during role selection

            User savedUser = userRepository.save(newUser);
//            log.info("New OAuth2 user created successfully: {}", email);
            return savedUser;

        } catch (DataAccessException e) {
//            log.error("Failed to create OAuth2 user: {}", email, e);
            throw new UserCreationException("Failed to create OAuth2 user", e);
        } catch (Exception e) {
//            log.error("Unexpected error creating OAuth2 user: {}", email, e);
            throw new UserCreationException("Failed to create OAuth2 user due to unexpected error", e);
        }
    }
}
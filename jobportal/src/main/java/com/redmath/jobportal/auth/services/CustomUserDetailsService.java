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

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService extends DefaultOAuth2UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {

        try {
            return userRepository.findByEmail(email)
                    .map(user -> {
                        return new CustomUserDetails(user);
                    })
                    .orElseThrow(() -> {
                        return new UserNotFoundException("User not found with email: " + email);
                    });
        } catch (DataAccessException e) {
            throw new UserNotFoundException("Unable to load user due to database error", e);
        } catch (Exception e) {
            throw new UserNotFoundException("Unable to load user", e);
        }
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attributes = oAuth2User.getAttributes();

            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");

            if (email == null || email.trim().isEmpty()) {
                throw new OAuth2ProcessingException("Email is required for OAuth2 authentication");
            }

            try {
                Optional<User> userOptional = userRepository.findByEmail(email);
                User user = userOptional.orElseGet(() -> createOAuth2User(email, name));

                if (user.getRole() == null) {
                    return new CustomUserDetails(user, true, attributes);
                }

                return new CustomUserDetails(user, attributes);

            } catch (DataAccessException e) {
                throw new OAuth2ProcessingException("Database error during OAuth2 authentication", e);
            }

        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (OAuth2ProcessingException e) {
            throw new OAuth2AuthenticationException(e.getMessage());
        } catch (Exception e) {
            throw new OAuth2AuthenticationException("OAuth2 authentication failed");
        }
    }

    private User createOAuth2User(String email, String name) {

        try {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name != null ? name : "OAuth2 User");
            newUser.setPassword(""); // No password for OAuth users
            newUser.setProvider(AuthProvider.GOOGLE);
            // Note: Role will be set during role selection

            User savedUser = userRepository.save(newUser);
            return savedUser;

        } catch (DataAccessException e) {
            throw new UserCreationException("Failed to create OAuth2 user", e);
        } catch (Exception e) {
            throw new UserCreationException("Failed to create OAuth2 user due to unexpected error", e);
        }
    }
}
package com.redmath.jobportal;

import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import com.redmath.jobportal.config.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OAuth2SuccessHandlerTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private OAuth2AuthenticationToken oAuth2AuthenticationToken;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private Authentication nonOAuth2Authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    private User existingUser;
    private Map<String, Object> userAttributes;

    @BeforeEach
    void setUp() {
        // Set frontend URL using reflection
        ReflectionTestUtils.setField(oAuth2SuccessHandler, "frontendUrl", "http://localhost:3000");

        existingUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("John Doe")
                .role(Role.APPLICANT)
                .provider(AuthProvider.GOOGLE)
                .build();

        userAttributes = new HashMap<>();
        userAttributes.put("email", "test@example.com");
        userAttributes.put("name", "John Doe");
    }

    @Test
    void testOnAuthenticationSuccess_ExistingUserWithRole() throws Exception {
        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(userAttributes);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("mock-jwt-token");

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        verify(response).sendRedirect("http://localhost:3000/auth/success?token=mock-jwt-token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testOnAuthenticationSuccess_ExistingUserWithoutRole() throws Exception {
        existingUser.setRole(null);
        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(userAttributes);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("mock-jwt-token");

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        verify(response).sendRedirect("http://localhost:3000/auth/role-selection?token=mock-jwt-token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testOnAuthenticationSuccess_InvalidAuthenticationType() throws Exception {
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, nonOAuth2Authentication);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid authentication type");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testOnAuthenticationSuccess_NullAttributes() throws Exception {
        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(null);

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 provider didn't return user attributes");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testOnAuthenticationSuccess_EmptyAttributes() throws Exception {
        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(new HashMap<>());

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 provider didn't return user attributes");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testOnAuthenticationSuccess_NoEmailInAttributes() throws Exception {
        Map<String, Object> attributesWithoutEmail = new HashMap<>();
        attributesWithoutEmail.put("name", "John Doe");

        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attributesWithoutEmail);

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth2 provider");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testOnAuthenticationSuccess_EmptyEmailInAttributes() throws Exception {
        Map<String, Object> attributesWithEmptyEmail = new HashMap<>();
        attributesWithEmptyEmail.put("email", "");
        attributesWithEmptyEmail.put("name", "John Doe");

        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attributesWithEmptyEmail);

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth2 provider");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testOnAuthenticationSuccess_EmailFromAlternativeAttribute() throws Exception {
        Map<String, Object> attributesWithSubEmail = new HashMap<>();
        attributesWithSubEmail.put("sub", "test@example.com");
        attributesWithSubEmail.put("name", "John Doe");

        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attributesWithSubEmail);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("mock-jwt-token");

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        verify(userRepository).findByEmail("test@example.com");
        verify(response).sendRedirect("http://localhost:3000/auth/success?token=mock-jwt-token");
    }

}
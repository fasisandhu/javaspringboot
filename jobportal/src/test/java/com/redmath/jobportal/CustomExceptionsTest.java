package com.redmath.jobportal;

import com.redmath.jobportal.auth.exceptions.OAuth2ProcessingException;
import com.redmath.jobportal.auth.exceptions.UserCreationException;
import com.redmath.jobportal.auth.exceptions.UserRegistrationException;
import com.redmath.jobportal.exceptions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomExceptionsTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private WebRequest webRequest;

    @Test
    void testUserCreationException_WithMessageAndCause() {
        Exception cause = new RuntimeException("Database connection failed");
        UserCreationException exception = new UserCreationException("Failed to create user", cause);

        assertEquals("Failed to create user", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testUserCreationException_WithMessageOnly() {
        UserCreationException exception = new UserCreationException("User creation failed");

        assertEquals("User creation failed", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testUserCreationException_HandlerResponse() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        UserCreationException exception = new UserCreationException("User creation failed");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserCreationException(exception, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("User Creation Failed", response.getBody().getError());
        assertEquals("User creation failed", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testOAuth2ProcessingException_WithMessageAndCause() {
        Exception cause = new IllegalStateException("OAuth2 token invalid");
        OAuth2ProcessingException exception = new OAuth2ProcessingException("OAuth2 authentication failed", cause);

        assertEquals("OAuth2 authentication failed", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testOAuth2ProcessingException_WithMessageOnly() {
        OAuth2ProcessingException exception = new OAuth2ProcessingException("OAuth2 processing error");

        assertEquals("OAuth2 processing error", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testOAuth2ProcessingException_HandlerResponse() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        OAuth2ProcessingException exception = new OAuth2ProcessingException("OAuth2 authentication failed");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleOAuth2ProcessingException(exception, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("OAuth2 Authentication Failed", response.getBody().getError());
        assertEquals("OAuth2 authentication failed", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testUserRegistrationException_WithMessageAndCause() {
        Exception cause = new IllegalArgumentException("Invalid email format");
        UserRegistrationException exception = new UserRegistrationException("User registration failed", cause);

        assertEquals("User registration failed", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testUserRegistrationException_WithMessageOnly() {
        UserRegistrationException exception = new UserRegistrationException("Registration error");

        assertEquals("Registration error", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testUserRegistrationException_HandlerResponse() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        UserRegistrationException exception = new UserRegistrationException("User registration failed");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserRegistrationException(exception, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Registration Failed", response.getBody().getError());
        assertEquals("User registration failed", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testExceptionChaining_UserCreationException() {
        RuntimeException rootCause = new RuntimeException("Root cause");
        IllegalStateException intermediateCause = new IllegalStateException("Intermediate cause", rootCause);
        UserCreationException exception = new UserCreationException("Final message", intermediateCause);

        assertEquals("Final message", exception.getMessage());
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    void testExceptionChaining_OAuth2ProcessingException() {
        RuntimeException rootCause = new RuntimeException("Token expired");
        OAuth2ProcessingException exception = new OAuth2ProcessingException("OAuth2 error", rootCause);

        assertEquals("OAuth2 error", exception.getMessage());
        assertEquals(rootCause, exception.getCause());
        assertEquals("Token expired", exception.getCause().getMessage());
    }

    @Test
    void testExceptionChaining_UserRegistrationException() {
        IllegalArgumentException rootCause = new IllegalArgumentException("Invalid data");
        UserRegistrationException exception = new UserRegistrationException("Registration failed", rootCause);

        assertEquals("Registration failed", exception.getMessage());
        assertEquals(rootCause, exception.getCause());
        assertEquals("Invalid data", exception.getCause().getMessage());
    }

    @Test
    void testHandlerWithDifferentWebRequestPath() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/auth/register");

        UserCreationException exception = new UserCreationException("Database error");
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserCreationException(exception, webRequest);

        assertEquals("/api/auth/register", response.getBody().getPath());
    }

    @Test
    void testHandlerWithEmptyMessage() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        UserCreationException exception = new UserCreationException("");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserCreationException(exception, webRequest);

        assertEquals("", response.getBody().getMessage());
        assertEquals("User Creation Failed", response.getBody().getError());
    }

    @Test
    void testHandlerWithNullMessage() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        UserCreationException exception = new UserCreationException(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserCreationException(exception, webRequest);

        assertNull(response.getBody().getMessage());
        assertEquals("User Creation Failed", response.getBody().getError());
    }

    @Test
    void testOAuth2ProcessingException_WithLongMessage() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        String longMessage = "This is a very long error message that might occur during OAuth2 processing when multiple things go wrong and we need to provide detailed information";
        OAuth2ProcessingException exception = new OAuth2ProcessingException(longMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleOAuth2ProcessingException(exception, webRequest);

        assertEquals(longMessage, response.getBody().getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUserRegistrationException_WithSpecialCharacters() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        String messageWithSpecialChars = "Registration failed: user@example.com already exists!";
        UserRegistrationException exception = new UserRegistrationException(messageWithSpecialChars);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserRegistrationException(exception, webRequest);

        assertEquals(messageWithSpecialChars, response.getBody().getMessage());
        assertEquals("Registration Failed", response.getBody().getError());
    }

    @Test
    void testDuplicateEmailException_WithMessageOnly() {
        DuplicateEmailException exception = new DuplicateEmailException("Duplicate email found");

        assertEquals("Duplicate email found", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testDuplicateEmailException_HandlerResponse() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        DuplicateEmailException exception = new DuplicateEmailException("Email already exists");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDuplicateEmailException(exception, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Duplicate Email", response.getBody().getError());
        assertEquals("Email already exists", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testUserNotFoundException_HandlerResponse() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        UserNotFoundException exception = new UserNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserNotFoundException(exception, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Authentication Failed", response.getBody().getError());
        assertEquals("Invalid credentials", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testUnauthorizedAccessException_WithMessageOnly() {
        UnauthorizedAccessException exception = new UnauthorizedAccessException("Access denied");

        assertEquals("Access denied", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testUnauthorizedAccessException_HandlerResponse() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        UnauthorizedAccessException exception = new UnauthorizedAccessException("Access denied");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnauthorizedAccessException(exception, webRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Unauthorized Access", response.getBody().getError());
        assertEquals("Access denied", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testDuplicateApplicationException_WithMessageOnly() {
        DuplicateApplicationException exception = new DuplicateApplicationException("Duplicate application");

        assertEquals("Duplicate application", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testDuplicateApplicationException_HandlerResponse() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        DuplicateApplicationException exception = new DuplicateApplicationException("Application already exists");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDuplicateApplicationException(exception, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Duplicate Application", response.getBody().getError());
        assertEquals("Application already exists", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }
}
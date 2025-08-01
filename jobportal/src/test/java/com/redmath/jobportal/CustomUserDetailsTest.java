package com.redmath.jobportal;

import com.redmath.jobportal.auth.model.CustomUserDetails;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.model.AuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CustomUserDetailsTest {

    private User userWithRole;
    private User userWithoutRole;
    private Map<String, Object> oauthAttributes;

    @BeforeEach
    void setUp() {
        userWithRole = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded-password")
                .name("John Doe")
                .role(Role.APPLICANT)
                .provider(AuthProvider.LOCAL)
                .build();

        userWithoutRole = User.builder()
                .id(2L)
                .email("newuser@example.com")
                .password("encoded-password")
                .name("Jane Doe")
                .role(null)
                .provider(AuthProvider.GOOGLE)
                .build();

        oauthAttributes = new HashMap<>();
        oauthAttributes.put("email", "oauth@example.com");
        oauthAttributes.put("name", "OAuth User");
        oauthAttributes.put("sub", "123456789");
    }

    @Test
    void testConstructor_WithUserOnly() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertEquals(userWithRole, userDetails.getUser());
        assertFalse(userDetails.needsRoleSelection());
        assertTrue(userDetails.getAttributes().isEmpty());
    }

//    @Test
//    void testConstructor_WithUserAndNeedsRoleSelection() {
//        CustomUserDetails userDetails = new CustomUserDetails(userWithRole, true);
//
//        assertEquals(userWithRole, userDetails.getUser());
//        assertTrue(userDetails.needsRoleSelection());
//        assertTrue(userDetails.getAttributes().isEmpty());
//    }

    @Test
    void testConstructor_WithUserAndAttributes() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole, oauthAttributes);

        assertEquals(userWithRole, userDetails.getUser());
        assertFalse(userDetails.needsRoleSelection());
        assertEquals(oauthAttributes, userDetails.getAttributes());
    }

    @Test
    void testConstructor_WithUserWithoutRoleAndAttributes() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithoutRole, oauthAttributes);

        assertEquals(userWithoutRole, userDetails.getUser());
        assertTrue(userDetails.needsRoleSelection());
        assertEquals(oauthAttributes, userDetails.getAttributes());
    }

    @Test
    void testConstructor_WithAllParameters() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole, false, oauthAttributes);

        assertEquals(userWithRole, userDetails.getUser());
        assertFalse(userDetails.needsRoleSelection());
        assertEquals(oauthAttributes, userDetails.getAttributes());
    }

    @Test
    void testConstructor_WithNullAttributes() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole, null);

        assertEquals(userWithRole, userDetails.getUser());
        assertTrue(userDetails.getAttributes().isEmpty());
    }

    @Test
    void testGetAuthorities_WithRole() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_APPLICANT")));
    }

    @Test
    void testGetAuthorities_WithoutRole() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithoutRole);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertTrue(authorities.isEmpty());
    }

    @Test
    void testGetAuthorities_WithEmployerRole() {
        User employerUser = User.builder()
                .id(3L)
                .email("employer@example.com")
                .role(Role.EMPLOYER)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(employerUser);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_EMPLOYER")));
    }

    @Test
    void testGetPassword() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertEquals("encoded-password", userDetails.getPassword());
    }

    @Test
    void testGetUsername() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertEquals("test@example.com", userDetails.getUsername());
    }

    @Test
    void testGetName() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertEquals("test@example.com", userDetails.getName());
    }

    @Test
    void testIsAccountNonExpired() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertTrue(userDetails.isAccountNonExpired());
    }

    @Test
    void testIsAccountNonLocked() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertTrue(userDetails.isAccountNonLocked());
    }

    @Test
    void testIsCredentialsNonExpired() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    void testIsEnabled() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertTrue(userDetails.isEnabled());
    }

    @Test
    void testGetUser() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertEquals(userWithRole, userDetails.getUser());
    }

    @Test
    void testNeedsRoleSelection_WithRole() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertFalse(userDetails.needsRoleSelection());
    }

    @Test
    void testNeedsRoleSelection_WithoutRole() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithoutRole);

        assertTrue(userDetails.needsRoleSelection());
    }

//    @Test
//    void testNeedsRoleSelection_OverriddenValue() {
//        CustomUserDetails userDetails = new CustomUserDetails(userWithRole, true);
//
//        assertTrue(userDetails.needsRoleSelection());
//    }

    @Test
    void testGetAttributes_EmptyByDefault() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertTrue(userDetails.getAttributes().isEmpty());
    }

    @Test
    void testGetAttributes_WithOAuthAttributes() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole, oauthAttributes);

        assertEquals(3, userDetails.getAttributes().size());
        assertEquals("oauth@example.com", userDetails.getAttributes().get("email"));
        assertEquals("OAuth User", userDetails.getAttributes().get("name"));
        assertEquals("123456789", userDetails.getAttributes().get("sub"));
    }

    @Test
    void testUserDetailsImplementation() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole);

        assertNotNull(userDetails.getUsername());
        assertNotNull(userDetails.getPassword());
        assertNotNull(userDetails.getAuthorities());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void testOAuth2UserImplementation() {
        CustomUserDetails userDetails = new CustomUserDetails(userWithRole, oauthAttributes);

        assertNotNull(userDetails.getName());
        assertNotNull(userDetails.getAttributes());
        assertEquals("test@example.com", userDetails.getName());
    }

    @Test
    void testWithNullPassword() {
        User userWithNullPassword = User.builder()
                .id(4L)
                .email("oauth@example.com")
                .password(null)
                .name("OAuth User")
                .role(Role.APPLICANT)
                .provider(AuthProvider.GOOGLE)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(userWithNullPassword);

        assertNull(userDetails.getPassword());
        assertEquals("oauth@example.com", userDetails.getUsername());
    }

}
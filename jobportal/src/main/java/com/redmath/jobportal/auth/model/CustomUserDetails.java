package com.redmath.jobportal.auth.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomUserDetails implements UserDetails, OAuth2User {
    private final User user;
    private final boolean needsRoleSelection;
    private final Map<String, Object> attributes;

    public CustomUserDetails(User user) {
        this.user = user;
        this.needsRoleSelection = false;
        this.attributes = Collections.emptyMap();
    }

    public CustomUserDetails(User user, boolean needsRoleSelection) {
        this.user = user;
        this.needsRoleSelection = needsRoleSelection;
        this.attributes = Collections.emptyMap();
    }

    // New constructor for OAuth2 with attributes
    public CustomUserDetails(User user, Map<String, Object> attributes) {
        this.user = user;
        this.needsRoleSelection = user.getRole() == null;
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
    }

    public CustomUserDetails(User user, boolean needsRoleSelection, Map<String, Object> attributes) {
        this.user = user;
        this.needsRoleSelection = needsRoleSelection;
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public User getUser() {
        return user;
    }

    public boolean needsRoleSelection() {
        return needsRoleSelection;
    }
}
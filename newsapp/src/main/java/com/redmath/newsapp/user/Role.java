package com.redmath.newsapp.user;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public enum Role {
    USER,
    EDITOR,
    ADMIN;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> "ROLE_" + this.name());
    }
}

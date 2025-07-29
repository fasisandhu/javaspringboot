package com.redmath.jobportal.auth.dtos;


import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private Role role;
    private AuthProvider provider;
}

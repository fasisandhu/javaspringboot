package com.redmath.jobportal.auth.dtos;


import com.redmath.jobportal.auth.model.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private Role role;
}

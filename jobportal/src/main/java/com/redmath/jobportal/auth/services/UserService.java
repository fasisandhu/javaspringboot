package com.redmath.jobportal.auth.services;


import com.redmath.jobportal.auth.dtos.RegisterRequest;
import com.redmath.jobportal.auth.model.User;
import com.redmath.jobportal.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest request){
        User user=User.builder().email(request.getEmail()).username(request.getUsername()).role(request.getRole())
                .password(passwordEncoder.encode(request.getPassword())).build();

        userRepository.save(user);
    }

}

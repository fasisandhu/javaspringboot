package com.redmath.newsapp.auth;

import com.redmath.newsapp.dto.AuthResponse;
import com.redmath.newsapp.dto.LoginRequest;
import com.redmath.newsapp.dto.RegisterRequest;
import com.redmath.newsapp.security.JwtUtils;
import com.redmath.newsapp.user.Role;
import com.redmath.newsapp.user.User;
import com.redmath.newsapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthResponse register(RegisterRequest request)
    {
        if(userRepo.existsByEmail(request.getEmail()))
        {
            throw new RuntimeException("email already in use");
        }

        User user= User.builder().email(request.getEmail()).name(request.getName()).password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole()!=null?request.getRole(): Role.USER).build();

        userRepo.save(user);

        String token=jwtUtils.generateToken(user);
        return AuthResponse.builder().token(token).build();
    }

    public AuthResponse login(LoginRequest request){
        //authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword())); //authenticates the user credentials but doesnot return user object

        User user=userRepo.findByEmail(request.getEmail()).orElseThrow(()->new RuntimeException("Invalid User Credentials(Email)"));

        String token=jwtUtils.generateToken(user);

        return AuthResponse.builder().token(token).build();
    }

}

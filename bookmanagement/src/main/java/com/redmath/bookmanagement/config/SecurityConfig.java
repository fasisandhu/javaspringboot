package com.redmath.bookmanagement.config;


import com.redmath.bookmanagement.users.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    private final AppUserService appUserService;

    public SecurityConfig(AppUserService appUserService){
        this.appUserService=appUserService;
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
//        http.authorizeHttpRequests(auth->auth.requestMatchers(HttpMethod.GET, "/api/books/**").hasAnyRole("VIEWER", "EDITOR")
//                .requestMatchers(HttpMethod.POST, "/api/books/**").hasRole("EDITOR")
//                .requestMatchers(HttpMethod.PUT, "/api/books/**").hasRole("EDITOR")
//                .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("EDITOR")
//                .anyRequest().authenticated()
//        ).formLogin(Customizer.withDefaults()).httpBasic(Customizer.withDefaults()).userDetailsService(appUserService);
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.formLogin(Customizer.withDefaults()).httpBasic(Customizer.withDefaults());
        http.authorizeHttpRequests(config -> config.requestMatchers(HttpMethod.GET, "/api/books/**").hasAnyRole("VIEWER", "EDITOR")
                .requestMatchers(HttpMethod.POST, "/api/books/**").hasRole("EDITOR")
                .requestMatchers(HttpMethod.PUT, "/api/books/**").hasRole("EDITOR")
                .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("EDITOR")
                .anyRequest().authenticated());
        http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // ⚠ Plain-text passwords (for learning only)
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
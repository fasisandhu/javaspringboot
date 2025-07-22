package com.redmath.jobportal.config;


import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.redmath.jobportal.auth.services.CustomUserDetailsService;
import com.redmath.jobportal.auth.services.UserService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@OpenAPIDefinition(info = @Info(title = "Job Portal API", version = "v1"), security = @SecurityRequirement(name = "bearerAuth"))
public class ApiSecurityConfiguration {

    @Value("${jwt.signing.key}")
    private byte[] signingKey;

    private final CustomUserDetailsService userDetailsService;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, JwtEncoder jwtEncoder) throws Exception{
        httpSecurity.formLogin(config->config.successHandler((request, response, auth) -> {
                    long expirySeconds=3600;
                    JwtClaimsSet claims=JwtClaimsSet.builder().subject(auth.getName()).expiresAt(Instant.now().plusSeconds(expirySeconds)).build();
                    JwsHeader jwsHeader=JwsHeader.with(MacAlgorithm.HS256).build();
                    Jwt jwt=jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,claims));
                    String json="{\"token_type\":\"Bearer\",\"access_token\":\"" + jwt.getTokenValue()
                            + "\",\"expires_in\":" + expirySeconds + "}";
                    response.getWriter().print(json);
        }
        ));
        httpSecurity.authorizeHttpRequests(auth->auth.requestMatchers("/auth/register", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated());

        httpSecurity.oauth2ResourceServer(oauth->oauth.jwt(jwt-> jwt.jwtAuthenticationConverter(token->{
                var user=userDetailsService.loadUserByUsername(token.getSubject());
                return new JwtAuthenticationToken(token,user.getAuthorities());
        })));

//        httpSecurity.csrf(csrf->csrf.disable());
        httpSecurity.sessionManagement(sess->sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        httpSecurity.csrf(csrf->csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));




        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder()
    {
        return new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(signingKey, "RSA")));
    }

    @Bean
    public JwtDecoder jwtDecoder()
    {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(signingKey,"RSA")).build();
    }
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(List.of(authProvider));
    }

}

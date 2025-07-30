package com.redmath.jobportal.config;

import com.redmath.jobportal.auth.services.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@Import(JwtConfiguration.class)
@OpenAPIDefinition(
        info = @Info(title = "Job Portal API", version = "v1", description = "Job Portal API Documentation"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT Bearer token"
)
public class ApiSecurityConfiguration {

    private final CustomUserDetailsService customUserDetailsService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    @Value("${jwt.signing.key}")
    private byte[] signingKey;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, JwtEncoder jwtEncoder) throws Exception {
        // Enable CORS
        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        httpSecurity.formLogin(config -> config.successHandler((request, response, auth) -> {
            long expirySeconds = 3600;
            JwtClaimsSet claims = JwtClaimsSet.builder().subject(auth.getName()).expiresAt(Instant.now().plusSeconds(expirySeconds)).build();
            JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
            Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));
            String json = "{\"token_type\":\"Bearer\",\"access_token\":\"" + jwt.getTokenValue() + "\",\"expires_in\":" + expirySeconds + "}";
            response.getWriter().print(json);
        }).usernameParameter("email"));

        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/auth/register",
                        "/auth/api/register",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/swagger-config",
                        "/v3/api-docs/swagger-ui.html",
                        "/v3/api-docs/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs.yaml",
                        "/v3/api-docs.json",
                        "/v3/api-docs.yaml/**",
                        "/v3/api-docs.json/**",
                        "/v3/api-docs/**",
                        "/oauth2/**",
                        "/login/oauth2/**",
                        "/h2-console/**",
                        "/",
                        "/actuator/**"
                ).permitAll()
                // Allow role selection endpoints for authenticated users (even without roles)
                .requestMatchers("/api/auth/roles", "/api/auth/select-role").authenticated()
                // Remove the old endpoint
                // .requestMatchers("/auth/select-role").permitAll()
                .anyRequest().authenticated()
        );

        // Configure OAuth2 login
        httpSecurity.oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler)
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customUserDetailsService)
                )
        );

        httpSecurity.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
            var user = customUserDetailsService.loadUserByUsername(token.getSubject());
            return new JwtAuthenticationToken(token, user.getAuthorities());
        })));

        httpSecurity.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        httpSecurity.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(List.of(authProvider));
    }

}
package com.redmath.jobportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class JwtTokenTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void testGenerateAndParseToken() {
        // Generate Token
        String subjectEmail = "john@example.com";
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subjectEmail)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));

        String token = jwt.getTokenValue();
        assertThat(token).isNotBlank();

        // Decode Token
        Jwt decodedJwt = jwtDecoder.decode(token);
        assertThat(decodedJwt.getSubject()).isEqualTo(subjectEmail);
    }

    @Test
    void testTokenExpirationValidation() {
        // Generate Expired Token
        String subjectEmail = "expired@example.com";
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subjectEmail)
                .issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600)) // Already expired
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));

        String token = jwt.getTokenValue();

        // Attempt to Decode - should throw exception
        try {
            jwtDecoder.decode(token);
        } catch (JwtValidationException e) {
            assertThat(e.getMessage()).contains("Jwt expired");
        }
    }
}

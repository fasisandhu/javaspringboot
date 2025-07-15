package com.redmath.newsapp.security;

import com.redmath.newsapp.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;


@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private String jwtExpirationMs;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(User user){
        return Jwts.builder().setSubject(user.getEmail()).claim("role",user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+Long.parseLong(jwtExpirationMs))).
                signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();

    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

//    claims are piece of information stored in tokens
//    Registered claims (standard)
//    These are predefined claims with specific meanings, for example:
//     sub (subject): The principal/user the token is about.
//     iat (issued at): When the token was issued.
//     exp (expiration): When the token expires.
//     aud (audience): Who the token is intended for.
    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token,User user){
        final String username=extractUsername(token);
        return (username.equals(user.getEmail())&& !isTokenExpired(token)); //! because if token is expired it gets true,but should return false
    }

    private boolean isTokenExpired(String token){
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

}

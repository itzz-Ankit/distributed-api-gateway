package Util;


import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;

@Component
public class JwtUtil {





    @Value("${JWT_SECRET:dev-only-change-this-secret-32-bytes}")
    private String secret ;


    @Value("${JWT_EXPIRATION:86400000}")
    private Long expiration ;

    public String jwtToken(String email ) {
        Long current_time = System.currentTimeMillis();

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(current_time))
                .expiration(new Date(current_time + expiration))
                .signWith(hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)))
                .compact();


    }
    public String extractEmail(String token ){

        return Jwts.parser()
                .verifyWith(hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject() ;

    }










}

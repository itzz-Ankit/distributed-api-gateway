package com.platform.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;


@Service
public class JwtService {


    @Value("${jwt.secret}")
    private String secret ;


    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)
                ))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

    }
}

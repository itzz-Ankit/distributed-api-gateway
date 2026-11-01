package com.platform.gateway;

import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Collections;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {


    private final JwtService jwtService ;
    public  JwtAuthenticationManager (JwtService jwtService){
        this.jwtService = jwtService ;

    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication){


        if( authentication == null || authentication.getCredentials() == null) {
            return Mono.error( new BadCredentialsException("Missing jwt token ")) ;

        }

        String token = authentication.getCredentials().toString() ;
        try{

            String email = jwtService.extractEmail(token) ;
            Authentication authentication1 = new UsernamePasswordAuthenticationToken(email , null , Collections.emptyList()) ;
            return Mono.just(authentication1)  ;

        }
        catch ( JwtException | IllegalArgumentException e ){
            return Mono.error( new BadCredentialsException("invalid JWT token" , e ))  ;

        }


    }
}

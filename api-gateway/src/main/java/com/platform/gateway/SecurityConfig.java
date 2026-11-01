package com.platform.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


@Configuration
public class SecurityConfig {

    private static final Set<String> PUBLIC_PATHS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "/user/login",
                    "/user/signup",
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/prometheus"
            ))
    );

    private final JwtAuthenticationManager jwtAuthenticationManager ;
    public SecurityConfig(JwtAuthenticationManager jwtAuthenticationManager){
        this.jwtAuthenticationManager = jwtAuthenticationManager ;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(jwtAuthenticationManager)  ;

        jwtFilter.setServerAuthenticationConverter(exchange ->{
            // Login/signup must not try to authenticate an old or invalid
            // Authorization header inherited from Postman/another client.
            // They are public endpoints and should always reach user-service.
            String path = exchange.getRequest().getPath().value();
            if (PUBLIC_PATHS.contains(path)
                    || path.startsWith("/v3/api-docs")
                    || path.equals("/swagger-ui.html")
                    || path.startsWith("/swagger-ui/")) {
                return Mono.empty();
            }

            String header = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION)  ;

            if (header == null || !header.startsWith("Bearer ")) {
                return Mono.empty() ;
            }
            String token = header.substring(7) ;

            return Mono.just( new UsernamePasswordAuthenticationToken("jwt" , token ))  ;
        });
        return http.csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeExchange(auth -> auth
                        .pathMatchers(
                                "/user/login",
                                "/user/signup",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/actuator/health",
                                "/actuator/info" ,
                                "/actuator/prometheus"
                        ).permitAll()
                        .anyExchange().authenticated( ) )
                .addFilterAt(jwtFilter , SecurityWebFiltersOrder.AUTHENTICATION ) // Matlab: filter chain mein authentication wali position par is filter ko lagao.
                        .build() ;
    }


}

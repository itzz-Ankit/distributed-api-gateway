package Config;


import Security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter ;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter){
        this.jwtAuthFilter = jwtAuthFilter ;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity )
        throws Exception{

        httpSecurity.csrf(csrf -> csrf.disable()) ;

        httpSecurity.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        httpSecurity.addFilterBefore(jwtAuthFilter , UsernamePasswordAuthenticationFilter.class) ;



        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        new AntPathRequestMatcher("/user/signup"),
                        new AntPathRequestMatcher("/user/login"),
                        new AntPathRequestMatcher("/v3/api-docs"),
                        new AntPathRequestMatcher("/v3/api-docs/**"),
                        new AntPathRequestMatcher("/swagger-ui.html"),
                        new AntPathRequestMatcher("/swagger-ui/**"),
                        new AntPathRequestMatcher("/webjars/**"),
                        new AntPathRequestMatcher("/actuator/health"),
                        new AntPathRequestMatcher("/actuator/info")
                ).permitAll()
                .anyRequest().authenticated()
        );
        return httpSecurity.build()  ;
    }






    // SecurityConfig ka kaam hai tumhare poore user-service
    // ke security rules decide karna.


//    Ab SecurityConfig decide karegi ki in APIs ko kaun access kar sakta hai.
//
// ( 1. Signup — public)
//
//    Naya user account banane ke liye bina JWT token ke request bhej sakta hai.
//
//          (   2. Login — public )
//
//    User email aur password bhejega. Password correct hone par JWT token milega.
//
// ( 3. Profile — protected )
//
//    User ko login karke mila valid JWT token request ke saath bhejna hoga. Tabhi profile access kar sakega.
































}

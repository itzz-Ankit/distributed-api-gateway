package Security;

import Util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter  extends OncePerRequestFilter {

    private final JwtUtil jwtUtil ;

    public JwtAuthFilter(JwtUtil jwtUtil){
        this.jwtUtil = jwtUtil ;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/user/login")
                || path.equals("/user/signup");
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest httpServletRequest ,
            HttpServletResponse httpServletResponse ,
            FilterChain filterChain )
        throws ServletException , IOException{


        String authHeader = httpServletRequest.getHeader("Authorization") ;

        if( authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Authorization: Bearer abc123xyz
            // TO bearer hat jayega  and waki string mil jayegi


       try{
           String email = jwtUtil.extractEmail(token);

           UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email , null , Collections.emptyList()) ;
           SecurityContextHolder.getContext().setAuthentication(authentication) ;


       }
       catch (io.jsonwebtoken.JwtException | IllegalArgumentException e ){
           SecurityContextHolder.clearContext() ;

       }
       }
        filterChain.doFilter(httpServletRequest , httpServletResponse );
    }
}

package com.platform.userservice;


import Mapper.UserMapper;
import Util.JwtUtil;
import dto_request.LoginReq;
import dto_request.SignupReq;
import dto_response.AuthResponse;
import dto_response.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {




    private final UserRepository userRepository ;
    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil ;


    // constructor injection
    public UserService(UserRepository userRepository , PasswordEncoder passwordEncoder ,JwtUtil jwtUtil ) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder ;
        this.jwtUtil = jwtUtil ;

    }


    public UserResponse getUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.toResponse(user);
    }

    public UserResponse signup(SignupReq request ){

        if (userRepository.findByEmail(request.getEmail()).isPresent()){
    throw new ResponseStatusException(
            HttpStatus.CONFLICT ,
            "Email already registered "
    ) ;
        }

        User user = new User() ;

        user.setName(request.getName());
        user.setEmail(request.getEmail());
       user.setPassword(
               passwordEncoder.encode( request.getPassword())
       );

        User savedUser = userRepository.save(user)  ;

        return UserMapper.toResponse(savedUser);

    }

    public AuthResponse login( LoginReq request ){

        // email  , password

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.UNAUTHORIZED ,
                        "invalid email or password ")) ;

        if (!passwordEncoder.matches(request.getPassword() , (user.getPassword()))){
            throw new ResponseStatusException( HttpStatus.UNAUTHORIZED , "invalid email or password ")  ;


        }
        String token = jwtUtil.jwtToken(user.getEmail()) ;

        return new AuthResponse("login suceessful" , token  , "Bearer");
    }

}


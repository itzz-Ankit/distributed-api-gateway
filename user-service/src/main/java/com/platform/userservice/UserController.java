package com.platform.userservice;


import dto_request.LoginReq;
import dto_request.SignupReq;
import dto_response.AuthResponse;
import dto_response.UserResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import javax.validation.Valid;

@RestController
public class UserController {

    private final UserService userService ;


    //constructor injection
    public UserController( UserService userService) {
        this.userService = userService ;
    }

    @GetMapping("/user")
    public UserResponse getUserName(Authentication authentication) {
        return userService.getUser(authentication.getName());
    }

        @PostMapping("/user/signup")
    public UserResponse signup(@Valid @RequestBody SignupReq request){
        return userService.signup(request) ;
}
         @PostMapping("/user/login")
    public AuthResponse Login(@Valid @RequestBody LoginReq request){
        return userService.login(request) ;
}

}

package com.platform.userservice;

import Mapper.UserMapper;
import Util.JwtUtil;
import dto_request.LoginReq;
import dto_request.SignupReq;
import dto_response.AuthResponse;
import dto_response.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    @Test
    void signupStoresEncodedPasswordAndReturnsSafeProfile() {
        SignupReq request = new SignupReq();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("StrongPass123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });

        UserResponse response = userService.signup(request);

        assertEquals("user-1", response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("test@example.com", response.getEmail());
        verify(passwordEncoder).encode("StrongPass123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginReturnsJwtWhenPasswordMatches() {
        LoginReq request = new LoginReq();
        request.setEmail("test@example.com");
        request.setPassword("StrongPass123");

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword("encoded-password");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.jwtToken(request.getEmail())).thenReturn("signed.jwt.token");

        AuthResponse response = userService.login(request);

        assertEquals("signed.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
    }
}

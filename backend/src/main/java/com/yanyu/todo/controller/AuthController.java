package com.yanyu.todo.controller;

import com.yanyu.todo.dto.*;
import com.yanyu.todo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService=authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid@RequestBody RegisterRequest request){
        authService.register(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid@RequestBody LoginRequest request){
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePasswod(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request){
        String username = authentication.getName();
        authService.changePassword(username,request);
        return ApiResponse.success(null);
    }
}

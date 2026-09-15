package com.yanyu.todo.service;

import com.yanyu.todo.dto.AuthResponse;
import com.yanyu.todo.dto.ChangePasswordRequest;
import com.yanyu.todo.dto.LoginRequest;
import com.yanyu.todo.dto.RegisterRequest;
import com.yanyu.todo.entity.User;
import com.yanyu.todo.exception.AuthException;
import com.yanyu.todo.repository.UserRepository;
import com.yanyu.todo.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public  AuthService(UserRepository userRepository,PasswordEncoder passwordEncoder,JwtService jwtService){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtService=jwtService;
    }

    @Transactional
    public void register(RegisterRequest request){
        String username=request.username().trim();

        if(userRepository.existsByUsername(username)){
            throw new AuthException(HttpStatus.CONFLICT, "用户名已被使用");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        userRepository.save(new User(username,passwordHash));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request){
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,"用户名或密码错误"));

        if (!passwordEncoder.matches(request.password(),user.getPasswordHash())){
            throw new AuthException(HttpStatus.UNAUTHORIZED,"用户名或密码错误");
        }

        String token = jwtService.generate(user.getUsername());
        return new AuthResponse(token,user.getUsername());
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request){
        User user=userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,"用户不存在"));

        if (!passwordEncoder.matches(request.oldPassword(),user.getPasswordHash())){
            throw new AuthException(HttpStatus.BAD_REQUEST,"旧密码错误");
        }

        if (!request.newPassword().equals(request.confirmPassword())){
            throw new AuthException(HttpStatus.BAD_REQUEST,"两次输入的新密码不一致");
        }

        if (request.oldPassword().equals(request.newPassword())){
            throw new AuthException(HttpStatus.BAD_REQUEST,"新密码不能与旧密码相同");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

}

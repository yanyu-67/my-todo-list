package com.yanyu.todo.service;

import com.yanyu.todo.entity.User;
import com.yanyu.todo.exception.ResourceNotFoundException;
import com.yanyu.todo.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


//注意 `LocalCurrentUserService` 使用了 `@Profile("local")`，而当前 `application.yaml` 默认激活 `local`。
// 为了验证真实 JWT，需要切换到非 `local` 配置，或者把本地 Profile 改成一个明确的测试 Profile，
// 并确保同一时间不会有两个 `CurrentUserService` Bean。
@Service
@Profile("!local")
public class SecurityCurrentUserService implements CurrentUserService{
    private  final UserRepository userRepository;
    public SecurityCurrentUserService(UserRepository userRepository){
        this.userRepository=userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User requireCurrentUser(){
        String username= SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
}

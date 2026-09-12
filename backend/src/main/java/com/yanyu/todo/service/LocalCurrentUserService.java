package com.yanyu.todo.service;

import com.yanyu.todo.entity.User;
import com.yanyu.todo.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local")
public class LocalCurrentUserService implements CurrentUserService{
    private final UserRepository userRepository;
    @Value("${app.dev-user-id}")
    private Long devUserId;

    public LocalCurrentUserService(UserRepository userRepository){
        this.userRepository=userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User requireCurrentUser(){
        return userRepository.findById(devUserId)
                .orElseThrow(()->new RuntimeException("本地测试用户不存在"));
    }
}

package com.yanyu.todo.service;

import com.yanyu.todo.dto.AdminUserResponse;
import com.yanyu.todo.dto.UserRoleUpdateRequest;
import com.yanyu.todo.dto.UserStatusUpdateRequest;
import com.yanyu.todo.entity.User;
import com.yanyu.todo.entity.UserRole;
import com.yanyu.todo.exception.AuthException;
import com.yanyu.todo.exception.ResourceNotFoundException;
import com.yanyu.todo.repository.UserRepository;
import com.yanyu.todo.repository.TodoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;

    public AdminUserService(UserRepository userRepository, TodoRepository todoRepository){
        this.userRepository=userRepository;
        this.todoRepository=todoRepository;
    }
    
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(){
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse updateStatus(Long userId, UserStatusUpdateRequest request){
        User user=findUser(userId);
        user.setEnabled(request.enabled());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateRole(Long userId, UserRoleUpdateRequest request){
        User user = findUser(userId);
        UserRole role = parseRole(request.role());
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUser(userId);
        String currentUsername = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        if (user.getUsername().equals(currentUsername)){
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "不能删除当前登录的管理员账号"
            );
        }
        todoRepository.deleteAllByUserId(userId);
        userRepository.delete(user);
    }


    private User findUser(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private UserRole parseRole(String role){
        try{
            return UserRole.valueOf(role.trim().toUpperCase());
        }catch (IllegalArgumentException ex){
            throw new AuthException(HttpStatus.BAD_REQUEST,"角色只能是 ADMIN 或 USER");
        }
    }

    private AdminUserResponse toResponse(User user){
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getEnabled(),
                user.getCreatedAt()
        );
    }
}

package com.yanyu.todo.controller;

import com.yanyu.todo.dto.AdminUserResponse;
import com.yanyu.todo.dto.ApiResponse;
import com.yanyu.todo.dto.UserRoleUpdateRequest;
import com.yanyu.todo.dto.UserStatusUpdateRequest;
import com.yanyu.todo.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService){
        this.adminUserService=adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> listUsers(){
        return ApiResponse.success(adminUserService.listUsers());
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<AdminUserResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request){
        return ApiResponse.success(adminUserService.updateStatus(userId, request));
    }

    @PatchMapping("/{userId}/role")
    public ApiResponse<AdminUserResponse> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request){
        return ApiResponse.success(adminUserService.updateRole(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId){
        adminUserService.deleteUser(userId);
        return ApiResponse.success(null);
    }
}

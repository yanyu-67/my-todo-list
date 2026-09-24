package com.yanyu.todo.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRoleUpdateRequest(
        @NotBlank(message = "角色不能为空")
        String role
) {
}

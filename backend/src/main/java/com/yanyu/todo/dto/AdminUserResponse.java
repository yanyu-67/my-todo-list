package com.yanyu.todo.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String role,
        Boolean enabled,
        LocalDateTime createdAt
) {
}

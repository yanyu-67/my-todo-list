package com.yanyu.todo.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "启用状态不能为空")
        Boolean enabled
) {
}

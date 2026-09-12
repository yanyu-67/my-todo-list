package com.yanyu.todo.dto;

import jakarta.validation.constraints.NotNull;

public record TodoCompleteRequest (
        @NotNull(message = "完成状态不能为空") Boolean completed
){ }

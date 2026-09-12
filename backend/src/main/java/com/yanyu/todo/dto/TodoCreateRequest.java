package com.yanyu.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TodoCreateRequest (
        @NotBlank(message = "标题不能为空")
        @Size(max = 200,message = "标题不能超过200个字符") String title,
        @Size(max = 2000,message = "标题不能超过2000个字符") String description,
        boolean important,
        LocalDate dueDate
){ }

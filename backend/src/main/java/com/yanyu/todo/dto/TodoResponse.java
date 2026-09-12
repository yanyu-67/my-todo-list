package com.yanyu.todo.dto;

import com.yanyu.todo.entity.Todo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodoResponse (
        Long id, String title, String description,
        boolean completed, boolean important, LocalDate dueDate,
        LocalDateTime createdAt,LocalDateTime updatedAt){

    public  static TodoResponse from(Todo todo){
        return new TodoResponse(todo.getId(),todo.getTitle(),
                todo.getDescription(),todo.isCompleted(),todo.isImportant(),
                todo.getDueDate(),todo.getCreatedAt(),todo.getUpdatedAt());
    }
}

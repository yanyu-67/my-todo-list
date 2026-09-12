package com.yanyu.todo.service;

import com.yanyu.todo.dto.TodoCreateRequest;
import com.yanyu.todo.dto.TodoResponse;
import com.yanyu.todo.entity.Todo;
import com.yanyu.todo.entity.User;
import com.yanyu.todo.repository.TodoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.yanyu.todo.dto.TodoUpdateRequest;
import com.yanyu.todo.dto.TodoCompleteRequest;
import com.yanyu.todo.exception.ResourceNotFoundException;

import java.util.List;

@Service
@Transactional
public class TodoService {
    private final TodoRepository todoRepository;
    private final CurrentUserService currentUserService;

    public  TodoService(TodoRepository todoRepository,CurrentUserService currentUserService){
        this.todoRepository=todoRepository;
        this.currentUserService=currentUserService;
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> list(){
        User user =currentUserService.requireCurrentUser();
        return todoRepository.findAllByUserId(user.getId()).stream()
                .map(TodoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TodoResponse get(Long id){
        return TodoResponse.from(findOwnedTodo(id));
    }

    public TodoResponse create(TodoCreateRequest request){
        User user = currentUserService.requireCurrentUser();
        Todo todo=new Todo(request.title(),request.description(),user);
        todo.setImportant(Boolean.TRUE.equals(request.important()));
        todo.setDueDate(request.dueDate());
        return TodoResponse.from(todoRepository.save(todo));
    }
    public TodoResponse update(Long id, TodoUpdateRequest request) {
        Todo todo = findOwnedTodo(id);
        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setImportant(Boolean.TRUE.equals(request.important()));
        todo.setDueDate(request.dueDate());
        return TodoResponse.from(todoRepository.save(todo));
    }

    public TodoResponse complete(Long id, TodoCompleteRequest request) {
        Todo todo = findOwnedTodo(id);
        todo.setCompleted(request.completed());
        return TodoResponse.from(todoRepository.save(todo));
    }

    public void delete(Long id) {
        todoRepository.delete(findOwnedTodo(id));
    }

    private Todo findOwnedTodo(Long id) {
        User user = currentUserService.requireCurrentUser();
        return todoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在"));
    }
}

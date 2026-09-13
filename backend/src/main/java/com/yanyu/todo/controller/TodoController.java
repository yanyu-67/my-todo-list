package com.yanyu.todo.controller;

import com.yanyu.todo.dto.*;
import com.yanyu.todo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//表示这是一个 REST 接口控制器。相当于：@Controller+@ResponseBody，方法返回的对象会自动转换成 JSON，而不是跳转到 HTML 页面。
@RestController
//用于映射请求路径。表示这个控制器下的接口都以 /api/todos 开头。
@RequestMapping("/api/todos")
public class TodoController {
    private final TodoService todoService;

    public TodoController(TodoService todoService){
        this.todoService=todoService;
    }

    @GetMapping
    public ApiResponse<List<TodoResponse>> list(){
        return ApiResponse.success(todoService.list());
    }


  //@PathVariable：获取 URL 路径中的变量。
    @GetMapping("/{id}")
    public  ApiResponse<TodoResponse> get(@PathVariable Long id){
        return  ApiResponse.success(todoService.get(id));
    }


   // @Valid： 触发请求参数校验。
    //@RequestBody：把 HTTP 请求体中的 JSON 转换成 Java 对象。
//    HttpStatus.OK          // 200：普通请求成功
//    HttpStatus.CREATED     // 201：创建资源成功
//    HttpStatus.NO_CONTENT  // 204：成功，但没有响应内容
//    HttpStatus.BAD_REQUEST // 400：请求参数错误
//    HttpStatus.NOT_FOUND   // 404：资源不存在

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TodoResponse> create(@Valid @RequestBody TodoCreateRequest request){
        return ApiResponse.success(todoService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TodoResponse> update(@PathVariable Long id, @Valid @RequestBody TodoUpdateRequest request){
        return ApiResponse.success(todoService.update(id,request));
    }


    //@PatchMapping：映射 HTTP 的 PATCH 请求，通常用于部分更新。
    @PatchMapping("/{id}/complete")
    public ApiResponse<TodoResponse> complete(@PathVariable Long id, @Valid@RequestBody TodoCompleteRequest request){
        return ApiResponse.success(todoService.complete(id,request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        todoService.delete(id);
    }
}

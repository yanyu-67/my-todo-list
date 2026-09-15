package com.yanyu.todo.exception;

import com.yanyu.todo.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;



//表示这是一个全局 REST 异常处理类。相当于：@ControllerAdvice+@ResponseBody,作用如下：
//@ControllerAdvice：让这个类可以处理多个 Controller 抛出的异常。
//@ResponseBody：方法返回值直接写入 HTTP 响应体，并转换成 JSON。
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log= LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //指定某个方法负责处理哪一种异常。
    @ExceptionHandler(ResourceNotFoundException.class)
   // 指定 HTTP 响应状态码。
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> notFound(ResourceNotFoundException ex){
        return new ApiResponse<>(404,ex.getMessage(),null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validatiton(MethodArgumentNotValidException ex){
        String message=ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage())
                .orElse("请求参数不合法");
        return new ApiResponse<>(400,message,null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> other(Exception ex){
        log.error("未处理的服务器异常",ex);
        return new ApiResponse<>(500,"服务器内部错误",null);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> auth(AuthException ex){
        return ResponseEntity.status(ex.getStatus()).body(new ApiResponse<>(
                ex.getStatus().value(),ex.getMessage(),null
        ));
    }
}


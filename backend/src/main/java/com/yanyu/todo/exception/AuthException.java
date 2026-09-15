package com.yanyu.todo.exception;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import org.slf4j.Logger;

public class AuthException extends RuntimeException{
    private final HttpStatus status;
    private static final Logger log= LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public AuthException(HttpStatus status,String message){
        super(message);
        this.status=status;
    }

    public HttpStatus getStatus(){
        return status;
    }



}

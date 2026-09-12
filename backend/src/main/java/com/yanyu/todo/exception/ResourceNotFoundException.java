package com.yanyu.todo.exception;

public class ResourceNotFoundException extends RuntimeException {
    public  ResourceNotFoundException(String message){
        super(message);
    }
}

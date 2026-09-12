package com.yanyu.todo.service;

import com.yanyu.todo.entity.User;

public interface CurrentUserService {
    User requireCurrentUser();
}

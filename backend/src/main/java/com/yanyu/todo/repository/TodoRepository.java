package com.yanyu.todo.repository;

import com.yanyu.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo,Long> {
    List<Todo> findAllByUserId(Long userId);
    Optional<Todo> findByIdAndUserId(Long id,Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndCompleted(Long userId,boolean completed);
    long countByUserIdAndImportant(Long userId,boolean important);

}

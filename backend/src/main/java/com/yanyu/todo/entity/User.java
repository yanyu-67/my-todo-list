package com.yanyu.todo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true,length = 50)
    private  String username;

    @Column(name = "password_hash",nullable = false,length = 100)
    private  String passwordHash;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY)
    private List<Todo> todos=new ArrayList<>();

    protected User(){}

    public  User (String username,String passwordHash){
        this.username=username;
        this.passwordHash=passwordHash;
    }

    @PrePersist
    protected  void  onCreate(){
        createdAt=LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Todo> getTodos() {
        return todos;
    }

    public void setTodos(List<Todo> todos) {
        this.todos = todos;
    }
}

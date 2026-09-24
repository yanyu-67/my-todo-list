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

    //表示数据库里保存 `USER` / `ADMIN`，不要保存 `0` / `1`
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private UserRole role=UserRole.USER;

    @Column(nullable = false)
    private Boolean enabled = true;

    protected User(){}

    public  User (String username,String passwordHash){
        this.username=username;
        this.passwordHash=passwordHash;
        this.role=UserRole.USER;
        this.enabled=true;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

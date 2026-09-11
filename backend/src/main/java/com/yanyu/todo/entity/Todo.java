package com.yanyu.todo.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length =200 )
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private  boolean completed=false;

    @Column(nullable = false)
    private boolean important =false;

    private LocalDate dueDate;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    protected Todo(){}

    public Todo(String title,String description,User user){
        this.title=title;
        this.description=description;
        this.user=user;
    }

    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        createAt=now;
        updatedAt=now;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt=LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }
}

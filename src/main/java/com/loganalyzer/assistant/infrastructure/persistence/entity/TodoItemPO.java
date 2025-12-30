package com.loganalyzer.assistant.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Todo持久化对象
 */
@Data
@Entity
@Table(name = "todo_items")
public class TodoItemPO {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String content;
    
    @Column(nullable = false)
    private boolean completed;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(nullable = false)
    private int priority;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

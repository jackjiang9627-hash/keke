package com.keke.assistant.domain.entity;

import com.keke.assistant.domain.valueobject.TodoId;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待办事项实体 - 聚合根
 * 
 * DDD概念：聚合根（Aggregate Root）
 * - 今日待办的核心实体
 */
@Getter
public class TodoItem {
    
    private TodoId id;
    private String content;         // 待办内容
    private boolean completed;      // 是否完成
    private LocalDate dueDate;      // 所属日期
    private int priority;           // 优先级 (1-高, 2-中, 3-低)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private TodoItem() {}
    
    /**
     * 创建新待办
     */
    public static TodoItem create(String content, LocalDate dueDate, int priority) {
        TodoItem item = new TodoItem();
        item.id = TodoId.generate();
        item.content = content;
        item.completed = false;
        item.dueDate = dueDate;
        item.priority = priority;
        item.createdAt = LocalDateTime.now();
        item.updatedAt = LocalDateTime.now();
        return item;
    }
    
    /**
     * 创建今日待办
     */
    public static TodoItem createToday(String content, int priority) {
        return create(content, LocalDate.now(), priority);
    }
    
    /**
     * 重建待办（从持久化恢复）
     */
    public static TodoItem reconstitute(TodoId id, String content, boolean completed,
                                        LocalDate dueDate, int priority,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        TodoItem item = new TodoItem();
        item.id = id;
        item.content = content;
        item.completed = completed;
        item.dueDate = dueDate;
        item.priority = priority;
        item.createdAt = createdAt;
        item.updatedAt = updatedAt;
        return item;
    }
    
    /**
     * 更新待办内容
     */
    public void update(String content, int priority) {
        this.content = content;
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为完成
     */
    public void markCompleted() {
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为未完成
     */
    public void markUncompleted() {
        this.completed = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 切换完成状态
     */
    public void toggleCompleted() {
        this.completed = !this.completed;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 是否是今日待办
     */
    public boolean isToday() {
        return LocalDate.now().equals(dueDate);
    }
}

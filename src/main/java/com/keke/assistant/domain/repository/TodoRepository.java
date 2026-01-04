package com.keke.assistant.domain.repository;

import com.keke.assistant.domain.entity.TodoItem;
import com.keke.assistant.domain.valueobject.TodoId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 待办仓储接口
 * 
 * DDD概念：仓储（Repository）
 */
public interface TodoRepository {
    
    /**
     * 保存待办
     */
    TodoItem save(TodoItem todoItem);
    
    /**
     * 根据ID查找
     */
    Optional<TodoItem> findById(TodoId id);
    
    /**
     * 查找指定日期的待办
     */
    List<TodoItem> findByDate(LocalDate date);
    
    /**
     * 查找今日待办
     */
    default List<TodoItem> findToday() {
        return findByDate(LocalDate.now());
    }
    
    /**
     * 查找所有待办
     */
    List<TodoItem> findAll();
    
    /**
     * 根据ID删除
     */
    void deleteById(TodoId id);
    
    /**
     * 统计今日待办数量
     */
    long countToday();
    
    /**
     * 统计今日未完成数量
     */
    long countTodayUncompleted();
}

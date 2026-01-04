package com.keke.assistant.application.service;

import com.keke.assistant.application.dto.TodoInputDTO;
import com.keke.assistant.application.dto.TodoOutputDTO;
import com.keke.assistant.domain.entity.TodoItem;
import com.keke.assistant.domain.repository.TodoRepository;
import com.keke.assistant.domain.valueobject.TodoId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 待办应用服务
 * 
 * DDD概念：应用服务（Application Service）
 */
@Slf4j
@Service
@Transactional
public class TodoApplicationService {
    
    private final TodoRepository todoRepository;
    
    public TodoApplicationService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    /**
     * 添加待办
     */
    public TodoOutputDTO addTodo(TodoInputDTO input) {
        log.info("添加待办: content={}", input.getContent());
        
        LocalDate dueDate = input.getDueDate() != null ? input.getDueDate() : LocalDate.now();
        int priority = input.getPriority() != null ? input.getPriority() : 2;
        
        TodoItem todoItem = TodoItem.create(input.getContent(), dueDate, priority);
        TodoItem saved = todoRepository.save(todoItem);
        
        log.info("待办添加成功: id={}", saved.getId().value());
        return toOutputDTO(saved);
    }
    
    /**
     * 更新待办
     */
    public TodoOutputDTO updateTodo(String id, TodoInputDTO input) {
        log.info("更新待办: id={}", id);
        
        TodoItem todoItem = todoRepository.findById(TodoId.of(id))
            .orElseThrow(() -> new RuntimeException("待办不存在: " + id));
        
        int priority = input.getPriority() != null ? input.getPriority() : todoItem.getPriority();
        todoItem.update(input.getContent(), priority);
        
        TodoItem saved = todoRepository.save(todoItem);
        log.info("待办更新成功: id={}", saved.getId().value());
        
        return toOutputDTO(saved);
    }
    
    /**
     * 切换完成状态
     */
    public TodoOutputDTO toggleComplete(String id) {
        log.info("切换待办状态: id={}", id);
        
        TodoItem todoItem = todoRepository.findById(TodoId.of(id))
            .orElseThrow(() -> new RuntimeException("待办不存在: " + id));
        
        todoItem.toggleCompleted();
        TodoItem saved = todoRepository.save(todoItem);
        
        log.info("待办状态已切换: id={}, completed={}", id, saved.isCompleted());
        return toOutputDTO(saved);
    }
    
    /**
     * 删除待办
     */
    public void deleteTodo(String id) {
        log.info("删除待办: id={}", id);
        todoRepository.deleteById(TodoId.of(id));
    }
    
    /**
     * 获取今日待办
     */
    @Transactional(readOnly = true)
    public List<TodoOutputDTO> getTodayTodos() {
        return todoRepository.findToday().stream()
            .map(this::toOutputDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取指定日期的待办
     */
    @Transactional(readOnly = true)
    public List<TodoOutputDTO> getTodosByDate(LocalDate date) {
        return todoRepository.findByDate(date).stream()
            .map(this::toOutputDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有待办
     */
    @Transactional(readOnly = true)
    public List<TodoOutputDTO> getAllTodos() {
        return todoRepository.findAll().stream()
            .map(this::toOutputDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 转换为输出DTO
     */
    private TodoOutputDTO toOutputDTO(TodoItem item) {
        String priorityLabel = switch (item.getPriority()) {
            case 1 -> "高";
            case 3 -> "低";
            default -> "中";
        };
        
        return TodoOutputDTO.builder()
            .id(item.getId().value())
            .content(item.getContent())
            .completed(item.isCompleted())
            .dueDate(item.getDueDate())
            .priority(item.getPriority())
            .priorityLabel(priorityLabel)
            .isToday(item.isToday())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}

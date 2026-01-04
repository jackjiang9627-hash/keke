package com.keke.assistant.interfaces.rest;

import com.keke.assistant.application.dto.TodoInputDTO;
import com.keke.assistant.application.dto.TodoOutputDTO;
import com.keke.assistant.application.service.TodoApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 待办REST控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    
    private final TodoApplicationService todoApplicationService;
    
    public TodoController(TodoApplicationService todoApplicationService) {
        this.todoApplicationService = todoApplicationService;
    }
    
    /**
     * 添加待办
     */
    @PostMapping
    public ResponseEntity<TodoOutputDTO> addTodo(@RequestBody TodoInputDTO input) {
        log.info("添加待办请求: {}", input.getContent());
        TodoOutputDTO result = todoApplicationService.addTodo(input);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 更新待办
     */
    @PutMapping("/{id}")
    public ResponseEntity<TodoOutputDTO> updateTodo(
            @PathVariable String id,
            @RequestBody TodoInputDTO input) {
        log.info("更新待办请求: id={}", id);
        TodoOutputDTO result = todoApplicationService.updateTodo(id, input);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 切换完成状态
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TodoOutputDTO> toggleComplete(@PathVariable String id) {
        log.info("切换待办状态: id={}", id);
        TodoOutputDTO result = todoApplicationService.toggleComplete(id);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除待办
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable String id) {
        log.info("删除待办请求: id={}", id);
        todoApplicationService.deleteTodo(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取今日待办
     */
    @GetMapping("/today")
    public ResponseEntity<List<TodoOutputDTO>> getTodayTodos() {
        List<TodoOutputDTO> results = todoApplicationService.getTodayTodos();
        return ResponseEntity.ok(results);
    }
    
    /**
     * 获取指定日期的待办
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<TodoOutputDTO>> getTodosByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TodoOutputDTO> results = todoApplicationService.getTodosByDate(date);
        return ResponseEntity.ok(results);
    }
    
    /**
     * 获取所有待办
     */
    @GetMapping
    public ResponseEntity<List<TodoOutputDTO>> getAllTodos() {
        List<TodoOutputDTO> results = todoApplicationService.getAllTodos();
        return ResponseEntity.ok(results);
    }
}

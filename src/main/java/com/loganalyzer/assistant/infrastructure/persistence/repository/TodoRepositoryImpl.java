package com.loganalyzer.assistant.infrastructure.persistence.repository;

import com.loganalyzer.assistant.domain.entity.TodoItem;
import com.loganalyzer.assistant.domain.repository.TodoRepository;
import com.loganalyzer.assistant.domain.valueobject.TodoId;
import com.loganalyzer.assistant.infrastructure.persistence.entity.TodoItemPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Todo仓储实现
 */
@Slf4j
@Repository
public class TodoRepositoryImpl implements TodoRepository {
    
    private final TodoItemJpaRepository jpaRepository;
    
    public TodoRepositoryImpl(TodoItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public TodoItem save(TodoItem todoItem) {
        TodoItemPO po = toPO(todoItem);
        TodoItemPO saved = jpaRepository.save(po);
        return toDomain(saved);
    }
    
    @Override
    public Optional<TodoItem> findById(TodoId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }
    
    @Override
    public List<TodoItem> findByDate(LocalDate date) {
        return jpaRepository.findByDueDateOrderByPriorityAscCreatedAtDesc(date).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TodoItem> findAll() {
        return jpaRepository.findAll().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(TodoId id) {
        jpaRepository.deleteById(id.value());
    }
    
    @Override
    public long countToday() {
        return jpaRepository.countByDate(LocalDate.now());
    }
    
    @Override
    public long countTodayUncompleted() {
        return jpaRepository.countUncompletedByDate(LocalDate.now());
    }
    
    private TodoItemPO toPO(TodoItem item) {
        TodoItemPO po = new TodoItemPO();
        po.setId(item.getId().value());
        po.setContent(item.getContent());
        po.setCompleted(item.isCompleted());
        po.setDueDate(item.getDueDate());
        po.setPriority(item.getPriority());
        po.setCreatedAt(item.getCreatedAt());
        po.setUpdatedAt(item.getUpdatedAt());
        return po;
    }
    
    private TodoItem toDomain(TodoItemPO po) {
        return TodoItem.reconstitute(
            TodoId.of(po.getId()),
            po.getContent(),
            po.isCompleted(),
            po.getDueDate(),
            po.getPriority(),
            po.getCreatedAt(),
            po.getUpdatedAt()
        );
    }
}

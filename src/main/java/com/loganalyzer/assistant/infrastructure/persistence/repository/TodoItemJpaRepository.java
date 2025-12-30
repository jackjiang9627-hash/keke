package com.loganalyzer.assistant.infrastructure.persistence.repository;

import com.loganalyzer.assistant.infrastructure.persistence.entity.TodoItemPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Todo JPA仓储接口
 */
public interface TodoItemJpaRepository extends JpaRepository<TodoItemPO, String> {
    
    List<TodoItemPO> findByDueDateOrderByPriorityAscCreatedAtDesc(LocalDate dueDate);
    
    @Query("SELECT COUNT(t) FROM TodoItemPO t WHERE t.dueDate = :date")
    long countByDate(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(t) FROM TodoItemPO t WHERE t.dueDate = :date AND t.completed = false")
    long countUncompletedByDate(@Param("date") LocalDate date);
}

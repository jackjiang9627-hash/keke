package com.keke.assistant.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Todo输出DTO
 */
@Data
@Builder
public class TodoOutputDTO {
    private String id;
    private String content;
    private boolean completed;
    private LocalDate dueDate;
    private int priority;
    private String priorityLabel;  // 高/中/低
    private boolean isToday;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

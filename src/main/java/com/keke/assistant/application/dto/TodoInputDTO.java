package com.keke.assistant.application.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Todo输入DTO
 */
@Data
public class TodoInputDTO {
    private String content;
    private LocalDate dueDate;  // 可选，默认今天
    private Integer priority;   // 1-高, 2-中, 3-低，默认2
}

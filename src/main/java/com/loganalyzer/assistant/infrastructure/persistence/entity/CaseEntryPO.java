package com.loganalyzer.assistant.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 案例持久化对象
 */
@Data
@Entity
@Table(name = "case_entries")
public class CaseEntryPO {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String summary;
    
    @Column(length = 500)
    private String hyperlink;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "module_name", nullable = false)
    private String moduleName;
    
    @Column(columnDefinition = "TEXT")
    private String embedding;  // JSON格式存储向量
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

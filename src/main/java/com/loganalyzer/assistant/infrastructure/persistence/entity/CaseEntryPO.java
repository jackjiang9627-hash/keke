package com.loganalyzer.assistant.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
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
    
    // === AI总结相关字段 ===
    @Column(length = 500)
    private String tags;  // 标签（逗号分隔）
    
    @Column(length = 20)
    private String source;  // 来源：MANUAL/AI_CHAT
    
    @Column(name = "original_conversation", columnDefinition = "TEXT")
    private String originalConversation;  // 原始对话记录(JSON)
    
    // === 遗忘曲线复习相关字段 ===
    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;  // 下次复习日期
    
    @Column(name = "review_count")
    private Integer reviewCount;  // 复习次数
    
    @Column(name = "mastery_level")
    private Integer masteryLevel;  // 熟练度 1-5
    
    @Column(name = "last_review_time")
    private LocalDateTime lastReviewTime;  // 上次复习时间
    
    @Column(name = "review_enabled")
    private Boolean reviewEnabled;  // 是否启用复习
}

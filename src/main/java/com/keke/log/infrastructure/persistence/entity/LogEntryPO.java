package com.keke.log.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 日志持久化实体 - 数据库映射
 * 
 * DDD概念：持久化对象（PO）
 * - 与数据库表结构对应
 * - 与领域实体分离
 * - 由基础设施层管理
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "log_entries")
public class LogEntryPO {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "cleaned_content", columnDefinition = "TEXT")
    private String cleanedContent;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "application", length = 100)
    private String application;

    @Column(name = "host", length = 100)
    private String host;

    @Column(name = "environment", length = 50)
    private String environment;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "cleaned")
    private boolean cleaned;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON格式存储
}

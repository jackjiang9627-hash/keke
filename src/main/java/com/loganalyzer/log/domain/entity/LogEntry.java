package com.loganalyzer.log.domain.entity;

import com.loganalyzer.log.domain.valueobject.CleansingResult;
import com.loganalyzer.log.domain.valueobject.LogId;
import com.loganalyzer.log.domain.valueobject.LogLevel;
import com.loganalyzer.log.domain.valueobject.LogSource;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志条目 - 聚合根
 * 
 * DDD概念：聚合根（Aggregate Root）
 * - 聚合的入口点，外部只能通过聚合根访问聚合内的对象
 * - 保证聚合内部的一致性
 * - 拥有全局唯一标识（LogId）
 * 
 * 本聚合包含：
 * - LogEntry（聚合根）
 * - LogLevel（值对象）
 * - LogSource（值对象）
 * - CleansingResult（值对象）
 */
public class LogEntry {

    // Getters
    @Getter
    private LogId id;                        // 唯一标识
    @Getter
    private String rawContent;               // 原始日志内容
    @Getter
    private String cleanedContent;           // 清洗后的内容
    @Getter
    private LogLevel level;                  // 日志级别
    @Getter
    private LogSource source;                // 日志来源
    @Getter
    private LocalDateTime timestamp;         // 日志时间戳
    @Getter
    private LocalDateTime createdAt;         // 创建时间
    @Getter
    private boolean cleaned;                 // 是否已清洗
    private Map<String, String> metadata;    // 元数据

    // 私有构造函数，通过工厂方法创建
    private LogEntry() {
        this.metadata = new HashMap<>();
    }

    /**
     * 工厂方法：创建原始日志条目
     * 
     * DDD概念：工厂方法（Factory Method）
     * - 封装复杂的创建逻辑
     * - 保证实体创建时的有效性
     */
    public static LogEntry createRaw(String rawContent, LogSource source) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Raw content cannot be empty");
        }
        
        LogEntry entry = new LogEntry();
        entry.id = LogId.generate();
        entry.rawContent = rawContent;
        entry.source = source;
        entry.timestamp = LocalDateTime.now();
        entry.createdAt = LocalDateTime.now();
        entry.level = LogLevel.INFO;  // 默认级别，清洗后会更新
        entry.cleaned = false;
        return entry;
    }

    /**
     * 重建日志条目（从持久化层恢复）
     */
    public static LogEntry reconstitute(String id, String rawContent, String cleanedContent,
                                        LogLevel level, LogSource source,
                                        LocalDateTime timestamp, LocalDateTime createdAt,
                                        boolean cleaned, Map<String, String> metadata) {
        LogEntry entry = new LogEntry();
        entry.id = LogId.of(id);
        entry.rawContent = rawContent;
        entry.cleanedContent = cleanedContent;
        entry.level = level;
        entry.source = source;
        entry.timestamp = timestamp;
        entry.createdAt = createdAt;
        entry.cleaned = cleaned;
        entry.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        return entry;
    }

    /**
     * 应用清洗结果
     * 
     * DDD概念：领域行为（Domain Behavior）
     * - 业务逻辑封装在实体内部
     * - 保持实体状态的一致性
     */
    public void applyCleansing(CleansingResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Cleansing result cannot be null");
        }
        
        if (result.isSuccess()) {
            this.cleanedContent = result.getCleanedContent();
            this.cleaned = true;
            
            // 从提取的字段中解析日志级别
            Map<String, String> fields = result.getExtractedFields();
            if (fields.containsKey("level")) {
                this.level = LogLevel.fromString(fields.get("level"));
            }
            if (fields.containsKey("timestamp")) {
                // 可以进一步解析时间戳
                this.metadata.put("parsedTimestamp", fields.get("timestamp"));
            }
            
            // 保存所有提取的字段到元数据
            this.metadata.putAll(fields);
        }
    }

    /**
     * 判断日志是否为错误级别
     */
    public boolean isError() {
        return level.isHigherOrEqualThan(LogLevel.ERROR);
    }

    /**
     * 判断日志是否为警告及以上级别
     */
    public boolean isWarningOrAbove() {
        return level.isHigherOrEqualThan(LogLevel.WARN);
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, String value) {
        if (key != null && !key.isBlank()) {
            this.metadata.put(key, value);
        }
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * 获取用于显示的内容
     */
    public String getDisplayContent() {
        return cleaned && cleanedContent != null ? cleanedContent : rawContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return id.equals(logEntry.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "id=" + id +
                ", level=" + level +
                ", source=" + source +
                ", cleaned=" + cleaned +
                '}';
    }
}

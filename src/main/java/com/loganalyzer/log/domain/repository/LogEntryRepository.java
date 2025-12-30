package com.loganalyzer.log.domain.repository;

import com.loganalyzer.log.domain.entity.LogEntry;
import com.loganalyzer.log.domain.valueobject.LogId;
import com.loganalyzer.log.domain.valueobject.LogLevel;
import com.loganalyzer.log.domain.valueobject.LogSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 日志仓储接口 - 领域层定义
 * 
 * DDD概念：仓储（Repository）
 * - 领域层只定义接口，不关心具体实现
 * - 提供聚合根的持久化能力
 * - 基础设施层负责具体实现
 * 
 * 这种设计遵循依赖倒置原则（DIP）：
 * - 高层模块（领域层）不依赖低层模块（基础设施层）
 * - 两者都依赖抽象（本接口）
 */
public interface LogEntryRepository {

    /**
     * 保存日志条目
     */
    LogEntry save(LogEntry logEntry);

    /**
     * 批量保存日志条目
     */
    List<LogEntry> saveAll(List<LogEntry> logEntries);

    /**
     * 根据ID查找
     */
    Optional<LogEntry> findById(LogId id);

    /**
     * 根据ID删除
     */
    void deleteById(LogId id);

    /**
     * 查找所有日志
     */
    List<LogEntry> findAll();

    /**
     * 分页查询
     */
    List<LogEntry> findAll(int page, int size);

    /**
     * 根据日志级别查找
     */
    List<LogEntry> findByLevel(LogLevel level);

    /**
     * 根据日志来源查找
     */
    List<LogEntry> findBySource(LogSource source);

    /**
     * 根据应用名称查找
     */
    List<LogEntry> findByApplication(String application);

    /**
     * 根据时间范围查找
     */
    List<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 查找未清洗的日志
     */
    List<LogEntry> findUncleaned();

    /**
     * 查找错误级别及以上的日志
     */
    List<LogEntry> findErrors();

    /**
     * 统计总数
     */
    long count();

    /**
     * 根据级别统计数量
     */
    long countByLevel(LogLevel level);
}

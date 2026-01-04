package com.keke.log.domain.valueobject;

/**
 * 日志级别 - 值对象
 * 
 * DDD概念：值对象（Value Object）
 * - 没有唯一标识
 * - 不可变
 * - 通过属性值来判断相等性
 */
public enum LogLevel {
    
    TRACE(0, "追踪"),
    DEBUG(1, "调试"),
    INFO(2, "信息"),
    WARN(3, "警告"),
    ERROR(4, "错误"),
    FATAL(5, "致命");

    private final int priority;
    private final String description;

    LogLevel(int priority, String description) {
        this.priority = priority;
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 判断当前级别是否高于等于指定级别
     */
    public boolean isHigherOrEqualThan(LogLevel other) {
        return this.priority >= other.priority;
    }

    /**
     * 从字符串解析日志级别
     */
    public static LogLevel fromString(String level) {
        if (level == null || level.isBlank()) {
            return INFO;
        }
        try {
            return LogLevel.valueOf(level.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return INFO;
        }
    }
}

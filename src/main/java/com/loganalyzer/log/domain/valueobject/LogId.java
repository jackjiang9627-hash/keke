package com.loganalyzer.log.domain.valueobject;

import java.util.Objects;

/**
 * 日志ID - 值对象
 * 
 * DDD概念：值对象作为实体的标识
 * - 封装ID生成逻辑
 * - 不可变
 */
public final class LogId {

    private final String value;

    public LogId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LogId cannot be null or blank");
        }
        this.value = value;
    }

    /**
     * 生成新的日志ID
     */
    public static LogId generate() {
        return new LogId(java.util.UUID.randomUUID().toString());
    }

    /**
     * 从已有值创建
     */
    public static LogId of(String value) {
        return new LogId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogId logId = (LogId) o;
        return Objects.equals(value, logId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

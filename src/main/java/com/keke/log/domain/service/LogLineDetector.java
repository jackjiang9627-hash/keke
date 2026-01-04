package com.keke.log.domain.service;

import org.springframework.stereotype.Service;

/**
 * 日志行检测领域服务
 * 
 * DDD概念：领域服务（Domain Service）
 * - 封装日志行识别的业务规则
 * - 判断一行文本是否是新日志的开始
 * - 这是纯业务逻辑，不依赖任何技术实现
 * 
 * 业务规则：
 * 1. 以时间戳开头的行是新日志
 * 2. 以日志级别开头的行是新日志
 * 3. 以JSON对象开头的行是新日志
 */
@Service
public class LogLineDetector {

    /**
     * 判断是否是新日志行的开始
     * 
     * @param line 待检测的文本行
     * @return true表示是新日志的开始，false表示是上一条日志的延续（如堆栈跟踪）
     */
    public boolean isNewLogLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        return matchesTimestampPattern(line) 
            || matchesLogLevelPattern(line) 
            || matchesJsonPattern(line);
    }
    
    /**
     * 匹配时间戳模式
     * 支持格式：
     * - 2024-01-01 10:00:00
     * - 2024/01/01 10:00:00
     * - Jan 01, 2024
     */
    private boolean matchesTimestampPattern(String line) {
        // ISO日期格式: 2024-01-01 或 2024/01/01
        if (line.matches("^\\d{4}[-/]\\d{2}[-/]\\d{2}.*")) {
            return true;
        }
        // 英文日期格式: Jan 01, 2024
        if (line.matches("^[A-Z][a-z]{2} \\d{1,2}, \\d{4}.*")) {
            return true;
        }
        return false;
    }
    
    /**
     * 匹配日志级别模式
     * 支持格式：
     * - [INFO] message
     * - INFO message
     * - [ERROR] message
     */
    private boolean matchesLogLevelPattern(String line) {
        // 带方括号: [INFO], [ERROR] 等
        if (line.matches("^\\[(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)].*")) {
            return true;
        }
        // 不带方括号: INFO, ERROR 等（后面跟空格）
        if (line.matches("^(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\s+.*")) {
            return true;
        }
        return false;
    }
    
    /**
     * 匹配JSON模式
     * 支持格式：
     * - {"level": "INFO", ...}
     */
    private boolean matchesJsonPattern(String line) {
        return line.trim().startsWith("{");
    }
    
    /**
     * 判断是否是堆栈跟踪行
     * 堆栈跟踪通常以空格/tab + "at" 开头
     */
    public boolean isStackTraceLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.startsWith("at ") 
            || trimmed.startsWith("Caused by:")
            || trimmed.startsWith("...")
            || (trimmed.contains("Exception") && trimmed.contains(":"));
    }
}
